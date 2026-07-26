package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
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
@Import(MySqlContainerConfig::class, PointEarningPersistenceAdapter::class)
class PointEarningPersistenceAdapterContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var adapter: PointEarningPersistenceAdapter

    private val walletId: String = UUID.randomUUID().toString()
    private val policyId: String = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        entityManager.persist(PointWalletEntity(id = walletId, memberId = UUID.randomUUID().toString(), balance = BigDecimal.ZERO))
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
        entityManager.flush()
    }

    @Test
    fun `적립건을 저장하고 조회하면 저장한 값 그대로 복원된다`() {
        val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)), sourceReferenceId = "ORDER-1")

        adapter.save(earning = earning, walletId = walletId, policyId = policyId)
        entityManager.flush()
        entityManager.clear()

        val found: PointEarning = adapter.findById(earning.id)

        found.amount.value shouldBe BigDecimal(1_000)
        found.sourceReferenceId shouldBe "ORDER-1"
    }

    @Test
    fun `존재하지 않는 적립건을 조회하면 예외가 발생한다`() {
        shouldThrow<PointDomainException> { adapter.findById("no-such-earning") }
    }

    @Test
    fun `여러 적립건을 saveAll로 한 번에 저장할 수 있다`() {
        val earnings: List<PointEarning> = listOf(pointEarning(sourceReferenceId = "A"), pointEarning(sourceReferenceId = "B"))

        adapter.saveAll(earnings = earnings, walletId = walletId, policyId = policyId)
        entityManager.flush()
        entityManager.clear()

        adapter.findAllByIds(earnings.map { it.id }).size shouldBe 2
    }

    @Test
    fun `적립건 취소로 상태가 바뀌면 updateStatus로 반영된다`() {
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

    @Test
    fun `updateStatusAll 호출 시 존재하지 않는 적립건이 섞여있으면 예외가 발생한다`() {
        val notPersisted: PointEarning = pointEarning()

        shouldThrow<PointDomainException> { adapter.updateStatusAll(earnings = listOf(notPersisted), walletId = walletId) }
    }

    @Test
    fun `동일한 지갑·유형·출처참조값 조합의 적립건을 조회할 수 있다`() {
        val earning: PointEarning = pointEarning(earnType = EarnType.SYSTEM, sourceReferenceId = "ORDER-IDEMPOTENT")
        adapter.save(earning = earning, walletId = walletId, policyId = policyId)
        entityManager.flush()
        entityManager.clear()

        val found: PointEarning? =
            adapter.findByWalletIdAndEarnTypeAndSourceReferenceId(
                walletId = walletId,
                earnType = EarnType.SYSTEM,
                sourceReferenceId = "ORDER-IDEMPOTENT",
            )

        found?.id shouldBe earning.id
    }

    @Test
    fun `일치하는 조합이 없으면 null이 반환된다`() {
        adapter.findByWalletIdAndEarnTypeAndSourceReferenceId(
            walletId = walletId,
            earnType = EarnType.SYSTEM,
            sourceReferenceId = "no-such-source",
        ) shouldBe null
    }

    @Test
    fun `사용 가능한 적립건만 findRedeemableByWalletId로 조회된다`() {
        val redeemable: PointEarning = pointEarning(sourceReferenceId = "REDEEMABLE", period = expirationPeriod(365))
        adapter.save(earning = redeemable, walletId = walletId, policyId = policyId)
        entityManager.persist(
            PointEarningEntity(
                id = UUID.randomUUID().toString(),
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

    @Test
    fun `지갑의 모든 적립건을 상태 무관하게 조회할 수 있다`() {
        adapter.save(earning = pointEarning(sourceReferenceId = "A"), walletId = walletId, policyId = policyId)
        adapter.save(earning = pointEarning(sourceReferenceId = "B"), walletId = walletId, policyId = policyId)
        entityManager.flush()
        entityManager.clear()

        adapter.findAllByWalletId(walletId).size shouldBe 2
    }

    @Test
    fun `아직 만료되지 않은 적립건만 있으면 만료 후보 지갑 목록이 비어있다`() {
        val earning: PointEarning =
            pointEarning(earnedAt = LocalDateTime.now(), period = expirationPeriod(365), sourceReferenceId = "NOT-DUE")
        adapter.save(earning = earning, walletId = walletId, policyId = policyId)
        entityManager.flush()
        entityManager.clear()

        adapter.findExpiredCandidateWalletIds(LocalDateTime.now()).shouldBeEmpty()
    }

    @Test
    fun `만료일이 지난 ACTIVE 적립건이 있으면 만료 후보 지갑 목록과 만료 대상 적립건이 조회된다`() {
        val earningId: String = UUID.randomUUID().toString()
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

        adapter.findExpiredCandidateWalletIds(LocalDateTime.now()) shouldBe listOf(walletId)
        val expiring: List<PointEarning> = adapter.findExpiringByWalletId(walletId = walletId, now = LocalDateTime.now())
        expiring.map { it.id } shouldBe listOf(earningId)
    }
}
