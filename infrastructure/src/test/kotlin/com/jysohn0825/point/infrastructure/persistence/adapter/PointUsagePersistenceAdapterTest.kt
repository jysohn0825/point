package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.pointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.usageLine
import com.jysohn0825.point.infrastructure.key.MemoryDistributedKeyGenerator
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@DataJpaTest
@Import(PointUsagePersistenceAdapter::class, MemoryDistributedKeyGenerator::class)
class PointUsagePersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointUsagePersistenceAdapter,
) : BehaviorSpec({
        val walletId: String = Random.nextLong(0, Long.MAX_VALUE).toString()
        var earningId: String = ""

        beforeEach {
            val policyId: String = Random.nextLong(0, Long.MAX_VALUE).toString()
            earningId = Random.nextLong(0, Long.MAX_VALUE).toString()
            entityManager.persist(
                PointWalletEntity(
                    id = walletId,
                    memberId = UUID.randomUUID().toString(),
                    balance = BigDecimal(1_000),
                    holdingLimit = BigDecimal(1_000_000),
                ),
            )
            entityManager.persist(
                PointPolicyEntity(
                    id = policyId,
                    policyVersion = 1,
                    maxEarnPerTransaction = BigDecimal(50_000),
                    maxHoldingAmount = BigDecimal(1_000_000),
                    defaultExpirationDays = 365,
                    appliedAt = LocalDate.now().minusDays(1),
                    createdByAdminId = "admin-01",
                ),
            )
            entityManager.persist(
                PointEarningEntity(
                    id = earningId,
                    walletId = walletId,
                    policyId = policyId,
                    amount = BigDecimal(1_000),
                    remainingAmount = BigDecimal(700),
                    earnType = EarnType.SYSTEM.name,
                    sourceReferenceId = "ORDER-BASE",
                    earnedAt = LocalDateTime.now().minusDays(1),
                    expiresAt = LocalDateTime.now().plusDays(300),
                    status = EarningStatus.ACTIVE.name,
                ),
            )
            entityManager.flush()
        }

        Given("사용건을 저장할 때") {
            When("저장 후 조회하면") {
                Then("라인까지 포함해 복원된다") {
                    val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(300))))

                    adapter.save(usage = usage, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointUsage = adapter.findById(usage.id)

                    found.orderNumber shouldBe usage.orderNumber
                    found.lines.size shouldBe 1
                    found.lines[0].earningId shouldBe earningId
                    found.lines[0].amount shouldBe BigDecimal(300)
                }
            }
        }

        Given("존재하지 않는 사용건일 때") {
            When("id로 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { adapter.findById("no-such-usage") }
                }
            }
        }

        Given("지갑에 사용건이 저장되어 있을 때") {
            When("findAllByWalletId로 조회하면") {
                Then("지갑의 모든 사용건이 조회된다") {
                    adapter.save(
                        usage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(100)))),
                        walletId = walletId,
                    )
                    entityManager.flush()
                    entityManager.clear()

                    adapter.findAllByWalletId(walletId).size shouldBe 1
                }
            }
        }

        Given("적립건을 사용한 사용건이 저장되어 있을 때") {
            When("findLinesByEarningId로 조회하면") {
                Then("적립건 기준의 사용 추적 라인이 조회된다") {
                    val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(250))))
                    adapter.save(usage = usage, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val traces: List<EarningUsageTrace> = adapter.findLinesByEarningId(earningId)

                    traces.size shouldBe 1
                    traces[0].orderNumber shouldBe usage.orderNumber
                    traces[0].amount shouldBe BigDecimal(250)
                }
            }
        }

        Given("사용건이 저장되어 있을 때") {
            When("사용취소를 저장하면") {
                Then("취소 이력과 함께 사용건이 갱신된다") {
                    val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(300))))
                    adapter.save(usage = usage, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val reloaded: PointUsage = adapter.findById(usage.id)
                    val requestedLines: List<CancellationLine> =
                        listOf(
                            CancellationLine(
                                originalLine = reloaded.lines[0],
                                restoredAmount = BigDecimal(300),
                                restorationType = RestorationType.RESTORED,
                            ),
                        )
                    reloaded.cancel(requestedLines)

                    adapter.saveCancellation(
                        usage = reloaded,
                        walletId = walletId,
                        requestedLines = requestedLines,
                        cancellationId = Random.nextLong(0, Long.MAX_VALUE).toString(),
                        requestId = UUID.randomUUID().toString(),
                        canceledAt = LocalDateTime.now(),
                    )
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointUsage = adapter.findById(usage.id)

                    found.cancellationLines.size shouldBe 1
                    found.cancellationLines[0].restoredAmount shouldBe BigDecimal(300)
                    found.cancellationLines[0].restorationType shouldBe RestorationType.RESTORED
                }
            }
        }

        Given("이미 같은 requestId로 사용취소가 저장되어 있을 때") {
            When("동일 requestId로 다시(=재시도) 취소를 저장하면") {
                Then("DB 유니크 제약(uk_usage_cancellation_request) 위반으로 실패한다") {
                    val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(300))))
                    adapter.save(usage = usage, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val firstReloaded: PointUsage = adapter.findById(usage.id)
                    val firstLines: List<CancellationLine> =
                        listOf(
                            CancellationLine(
                                originalLine = firstReloaded.lines[0],
                                restoredAmount = BigDecimal(100),
                                restorationType = RestorationType.RESTORED,
                            ),
                        )
                    firstReloaded.cancel(firstLines)
                    adapter.saveCancellation(
                        usage = firstReloaded,
                        walletId = walletId,
                        requestedLines = firstLines,
                        cancellationId = Random.nextLong(0, Long.MAX_VALUE).toString(),
                        requestId = "duplicate-request-id",
                        canceledAt = LocalDateTime.now(),
                    )
                    entityManager.flush()
                    entityManager.clear()

                    // 부분취소 후 남은 200원 중 100원을 같은 requestId로 다시 취소 시도 — 도메인 상태 가드(FULLY_CANCELED
                    // 여부)만으로는 걸러지지 않는, 재시도로 인한 중복 부분취소 시나리오를 재현한다.
                    val secondReloaded: PointUsage = adapter.findById(usage.id)
                    val secondLines: List<CancellationLine> =
                        listOf(
                            CancellationLine(
                                originalLine = secondReloaded.lines[0],
                                restoredAmount = BigDecimal(100),
                                restorationType = RestorationType.RESTORED,
                            ),
                        )
                    secondReloaded.cancel(secondLines)

                    shouldThrow<ConstraintViolationException> {
                        adapter.saveCancellation(
                            usage = secondReloaded,
                            walletId = walletId,
                            requestedLines = secondLines,
                            cancellationId = Random.nextLong(0, Long.MAX_VALUE).toString(),
                            requestId = "duplicate-request-id",
                            canceledAt = LocalDateTime.now(),
                        )
                        entityManager.flush()
                    }
                }
            }
        }
    })
