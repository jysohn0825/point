package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@DataJpaTest
@Import(PointEarningPersistenceAdapter::class)
class PointEarningPersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointEarningPersistenceAdapter,
) : BehaviorSpec({
        val walletId: String = Random.nextLong(0, Long.MAX_VALUE).toString()
        val policyId: String = Random.nextLong(0, Long.MAX_VALUE).toString()

        beforeEach {
            entityManager.persist(
                PointWalletEntity(
                    id = walletId,
                    memberId = UUID.randomUUID().toString(),
                    balance = BigDecimal.ZERO,
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
            entityManager.flush()
        }

        Given("적립건을 저장할 때") {
            When("저장 후 조회하면") {
                Then("저장한 값 그대로 복원된다") {
                    val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)), sourceReferenceId = "ORDER-1")

                    adapter.save(earning = earning, walletId = walletId, policyId = policyId)
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointEarning = adapter.findById(earning.id)

                    found.amount.value shouldBe BigDecimal(1_000)
                    found.sourceReferenceId shouldBe "ORDER-1"
                }
            }
        }

        Given("존재하지 않는 적립건일 때") {
            When("id로 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { adapter.findById("no-such-earning") }
                }
            }
        }

        Given("여러 적립건을 저장할 때") {
            When("saveAll로 한 번에 저장하면") {
                Then("모두 저장된다") {
                    val earnings: List<PointEarning> = listOf(pointEarning(sourceReferenceId = "A"), pointEarning(sourceReferenceId = "B"))

                    adapter.saveAll(earnings = earnings, walletId = walletId, policyId = policyId)
                    entityManager.flush()
                    entityManager.clear()

                    adapter.findAllByIds(earnings.map { it.id }).size shouldBe 2
                }
            }
        }

        Given("적립건이 취소되었을 때") {
            When("updateStatus로 반영하면") {
                Then("상태가 갱신된다") {
                    val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                    adapter.save(earning = earning, walletId = walletId, policyId = policyId)
                    entityManager.flush()
                    entityManager.clear()

                    earning.cancelEarning()
                    adapter.updateStatus(earning = earning, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointEarning = adapter.findById(earning.id)
                    found.status shouldBe EarningStatus.CANCELED
                }
            }
        }

        Given("updateStatusAll 호출 대상에 존재하지 않는 적립건이 섞여있을 때") {
            When("updateStatusAll을 호출하면") {
                Then("예외가 발생한다") {
                    val notPersisted: PointEarning = pointEarning()

                    shouldThrow<PointDomainException> { adapter.updateStatusAll(earnings = listOf(notPersisted), walletId = walletId) }
                }
            }
        }

        Given("사용 가능한 적립건과 소진된 적립건이 함께 있을 때") {
            When("findRedeemableByWalletId로 조회하면") {
                Then("사용 가능한 적립건만 조회된다") {
                    val redeemable: PointEarning = pointEarning(sourceReferenceId = "REDEEMABLE", period = expirationPeriod(365))
                    adapter.save(earning = redeemable, walletId = walletId, policyId = policyId)
                    entityManager.persist(
                        PointEarningEntity(
                            id = Random.nextLong(0, Long.MAX_VALUE).toString(),
                            walletId = walletId,
                            policyId = policyId,
                            amount = BigDecimal(500),
                            remainingAmount = BigDecimal.ZERO,
                            earnType = EarnType.SYSTEM.name,
                            sourceReferenceId = "EXHAUSTED",
                            earnedAt = LocalDateTime.now(),
                            expiresAt = LocalDateTime.now().plusDays(365),
                            status = EarningStatus.EXHAUSTED.name,
                        ),
                    )
                    entityManager.flush()
                    entityManager.clear()

                    val result: List<PointEarning> = adapter.findRedeemableByWalletId(walletId)

                    result.map { it.id } shouldBe listOf(redeemable.id)
                }
            }
        }

        Given("지갑에 여러 적립건이 있을 때") {
            When("findAllByWalletId로 조회하면") {
                Then("상태 무관하게 모두 조회된다") {
                    adapter.save(earning = pointEarning(sourceReferenceId = "A"), walletId = walletId, policyId = policyId)
                    adapter.save(earning = pointEarning(sourceReferenceId = "B"), walletId = walletId, policyId = policyId)
                    entityManager.flush()
                    entityManager.clear()

                    adapter.findAllByWalletId(walletId).size shouldBe 2
                }
            }
        }

        Given("만료일이 지난 ACTIVE 적립건이 있을 때") {
            When("findExpiringByWalletId로 조회하면") {
                Then("만료 대상 적립건이 조회된다") {
                    val earningId: String = Random.nextLong(0, Long.MAX_VALUE).toString()
                    entityManager.persist(
                        PointEarningEntity(
                            id = earningId,
                            walletId = walletId,
                            policyId = policyId,
                            amount = BigDecimal(1_000),
                            remainingAmount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM.name,
                            sourceReferenceId = "EXPIRED-1",
                            earnedAt = LocalDateTime.now().minusDays(10),
                            expiresAt = LocalDateTime.now().minusDays(1),
                            status = EarningStatus.ACTIVE.name,
                        ),
                    )
                    entityManager.flush()
                    entityManager.clear()

                    val expiring: List<PointEarning> = adapter.findExpiringByWalletId(walletId = walletId, now = LocalDateTime.now())
                    expiring.map { it.id } shouldBe listOf(earningId)
                }
            }
        }
    })
