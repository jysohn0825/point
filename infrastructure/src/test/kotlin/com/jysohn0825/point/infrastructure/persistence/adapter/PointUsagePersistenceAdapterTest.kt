package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.fixture.usageLine
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Import(PointUsagePersistenceAdapter::class)
class PointUsagePersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointUsagePersistenceAdapter,
) : BehaviorSpec({
        val walletId: String = UUID.randomUUID().toString()
        var earningId: String = ""

        beforeEach {
            val policyId: String = UUID.randomUUID().toString()
            earningId = UUID.randomUUID().toString()
            entityManager.persist(PointWalletEntity(id = walletId, memberId = UUID.randomUUID().toString(), balance = BigDecimal(1_000)))
            entityManager.persist(
                PointPolicyEntity(
                    id = policyId,
                    policyVersion = 1,
                    maxEarnPerTransaction = BigDecimal(50_000),
                    maxHoldingAmount = BigDecimal(1_000_000),
                    defaultExpirationDays = 365,
                    appliedAt = LocalDateTime.now().minusDays(1),
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
                        cancellationId = UUID.randomUUID().toString(),
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
    })
