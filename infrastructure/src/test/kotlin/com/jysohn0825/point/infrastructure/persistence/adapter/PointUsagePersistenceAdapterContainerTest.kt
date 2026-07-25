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
import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import io.kotest.assertions.throwables.shouldThrow
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
@Import(MySqlContainerConfig::class, PointUsagePersistenceAdapter::class)
class PointUsagePersistenceAdapterContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var adapter: PointUsagePersistenceAdapter

    private val walletId: String = UUID.randomUUID().toString()
    private lateinit var earningId: String

    @BeforeEach
    fun setUp() {
        val policyId: String = UUID.randomUUID().toString()
        earningId = UUID.randomUUID().toString()
        entityManager.persist(PointWalletEntity(id = walletId, memberId = UUID.randomUUID().toString(), balance = 1_000L))
        entityManager.persist(
            PointPolicyEntity(
                id = policyId,
                policyVersion = 1,
                maxEarnPerTransaction = 50_000,
                maxHoldingAmount = 1_000_000L,
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
                amount = 1_000L,
                remainingAmount = 700L,
                earnType = EarnType.SYSTEM.name,
                sourceReferenceId = "ORDER-BASE",
                earnedAt = LocalDateTime.now().minusDays(1),
                expiresAt = LocalDateTime.now().plusDays(300),
                status = EarningStatus.ACTIVE.name,
            ),
        )
        entityManager.flush()
    }

    @Test
    fun `사용건을 저장하고 조회하면 라인까지 포함해 복원된다`() {
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

    @Test
    fun `존재하지 않는 사용건을 조회하면 예외가 발생한다`() {
        shouldThrow<PointDomainException> { adapter.findById("no-such-usage") }
    }

    @Test
    fun `지갑의 모든 사용건을 조회할 수 있다`() {
        adapter.save(
            usage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(100)))),
            walletId = walletId,
        )
        entityManager.flush()
        entityManager.clear()

        adapter.findAllByWalletId(walletId).size shouldBe 1
    }

    @Test
    fun `적립건 기준으로 사용 추적 라인을 조회할 수 있다`() {
        val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = earningId, amount = BigDecimal(250))))
        adapter.save(usage = usage, walletId = walletId)
        entityManager.flush()
        entityManager.clear()

        val traces: List<EarningUsageTrace> = adapter.findLinesByEarningId(earningId)

        traces.size shouldBe 1
        traces[0].orderNumber shouldBe usage.orderNumber
        traces[0].amount shouldBe BigDecimal(250)
    }

    @Test
    fun `사용취소를 저장하면 취소 이력과 함께 사용건이 갱신된다`() {
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
            reearnedEarningIds = listOf(null),
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
