package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig::class, PointPolicyPersistenceAdapter::class)
class PointPolicyPersistenceAdapterContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var adapter: PointPolicyPersistenceAdapter

    @Test
    fun `적용 가능한 정책이 없으면 예외가 발생한다`() {
        shouldThrow<PointDomainException> { adapter.getCurrent() }
    }

    @Test
    fun `정책을 저장하면 버전이 1부터 자동으로 채번된다`() {
        val policy: PointPolicy =
            pointPolicy(
                maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(50_000)),
                maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000)),
                defaultExpirationPeriod = expirationPeriod(365),
            )

        adapter.save(policy = policy, appliedAt = LocalDateTime.now().minusDays(1), createdByAdminId = "admin-01")
        entityManager.flush()
        entityManager.clear()

        val current: PointPolicy = adapter.getCurrent()

        current.maxEarnPerTransaction.value shouldBe BigDecimal(50_000)
        current.maxHoldingAmount.value shouldBe BigDecimal(1_000_000)
        current.defaultExpirationPeriod.days shouldBe 365L
    }

    @Test
    fun `여러 버전이 있으면 적용 시각이 지난 것 중 가장 최신 버전이 조회된다`() {
        adapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(500_000))),
            appliedAt = LocalDateTime.now().minusDays(10),
            createdByAdminId = "admin-01",
        )
        adapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(900_000))),
            appliedAt = LocalDateTime.now().minusDays(1),
            createdByAdminId = "admin-01",
        )
        adapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_500_000))),
            appliedAt = LocalDateTime.now().plusDays(30),
            createdByAdminId = "admin-01",
        )
        entityManager.flush()
        entityManager.clear()

        val current: PointPolicy = adapter.getCurrent()

        current.maxHoldingAmount.value shouldBe BigDecimal(900_000)
    }
}
