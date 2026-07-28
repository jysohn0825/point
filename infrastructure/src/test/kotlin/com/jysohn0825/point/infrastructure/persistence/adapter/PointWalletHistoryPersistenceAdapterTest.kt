package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.entity.pointWalletHistory
import com.jysohn0825.point.domain.vo.HistoryType
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletHistoryEntity
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
@Import(PointWalletHistoryPersistenceAdapter::class)
class PointWalletHistoryPersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointWalletHistoryPersistenceAdapter,
) : BehaviorSpec({
        val walletId: String = UUID.randomUUID().toString()
        val policyId: String = UUID.randomUUID().toString()
        val earningId: String = UUID.randomUUID().toString()

        beforeEach {
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
                    remainingAmount = BigDecimal(1_000),
                    earnType = "SYSTEM",
                    sourceReferenceId = "ORDER-1",
                    earnedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plusDays(365),
                    status = "ACTIVE",
                ),
            )
            entityManager.flush()
        }

        Given("지갑·정책·적립건이 준비되어 있을 때") {
            When("적립 히스토리를 저장하면") {
                Then("참조 FK와 함께 그대로 영속화된다") {
                    val history: PointWalletHistory =
                        pointWalletHistory(
                            historyType = HistoryType.EARN,
                            amount = BigDecimal(1_000),
                            balanceAfter = BigDecimal(1_000),
                            earningId = earningId,
                            usageId = null,
                            cancellationId = null,
                        )

                    adapter.save(history = history, walletId = walletId)
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointWalletHistoryEntity =
                        entityManager.find(PointWalletHistoryEntity::class.java, history.id)

                    found.walletId shouldBe walletId
                    found.historyType shouldBe "EARN"
                    found.amount shouldBe BigDecimal(1_000)
                    found.balanceAfter shouldBe BigDecimal(1_000)
                    found.earningId shouldBe earningId
                }
            }
        }
    })
