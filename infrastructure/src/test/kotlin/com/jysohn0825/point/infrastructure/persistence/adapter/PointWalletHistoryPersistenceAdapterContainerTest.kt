package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.entity.pointWalletHistory
import com.jysohn0825.point.domain.vo.HistoryType
import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletHistoryEntity
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig::class, PointWalletHistoryPersistenceAdapter::class)
class PointWalletHistoryPersistenceAdapterContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var adapter: PointWalletHistoryPersistenceAdapter

    private val walletId: String = UUID.randomUUID().toString()
    private val policyId: String = UUID.randomUUID().toString()
    private val earningId: String = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
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

    @Test
    fun `히스토리를 저장하면 참조 FK와 함께 그대로 영속화된다`() {
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
