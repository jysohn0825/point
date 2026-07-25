package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
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
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig::class, PointWalletPersistenceAdapter::class, PointPolicyPersistenceAdapter::class, FakeCacheExecutor::class)
class PointWalletPersistenceAdapterContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var adapter: PointWalletPersistenceAdapter

    @Autowired
    private lateinit var policyAdapter: PointPolicyPersistenceAdapter

    @Test
    fun `존재하지 않는 회원의 지갑을 잠금 조회하면 예외가 발생한다`() {
        shouldThrow<PointDomainException> { adapter.findByMemberIdForUpdate("no-such-member") }
    }

    @Test
    fun `개인 한도 예외가 없는 지갑은 정책의 보유한도가 적용된다`() {
        policyAdapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(2_000_000))),
            appliedAt = LocalDateTime.now().minusDays(1),
            createdByAdminId = "admin-01",
        )
        entityManager.persist(PointWalletEntity(id = UUID.randomUUID().toString(), memberId = "member-1", balance = 1_000L))
        entityManager.flush()
        entityManager.clear()

        val found: PointWallet = adapter.findByMemberIdForUpdate("member-1")

        found.balance.amount shouldBe BigDecimal(1_000L)
        found.holdingLimit.value shouldBe BigDecimal(2_000_000)
    }

    @Test
    fun `개인 한도 예외가 있는 지갑은 정책 대신 예외값이 적용된다`() {
        policyAdapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000))),
            appliedAt = LocalDateTime.now().minusDays(1),
            createdByAdminId = "admin-01",
        )
        entityManager.persist(
            PointWalletEntity(
                id = UUID.randomUUID().toString(),
                memberId = "member-2",
                balance = 0L,
                holdingLimitOverride = 5_000_000L,
            ),
        )
        entityManager.flush()
        entityManager.clear()

        val found: PointWallet = adapter.findByMemberId("member-2")!!

        found.holdingLimit.value shouldBe BigDecimal(5_000_000L)
    }

    @Test
    fun `존재하지 않는 회원을 findByMemberId로 조회하면 null이 반환된다`() {
        adapter.findByMemberId("unknown") shouldBe null
    }

    @Test
    fun `신규 지갑을 저장하면 개인 한도 예외 없이 저장된다`() {
        policyAdapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000))),
            appliedAt = LocalDateTime.now().minusDays(1),
            createdByAdminId = "admin-01",
        )
        val wallet: PointWallet = pointWallet(balance = balance(BigDecimal(3_000)))

        adapter.save(wallet = wallet, memberId = "member-3")
        entityManager.flush()
        entityManager.clear()

        val found: PointWallet = adapter.findByMemberIdForUpdate("member-3")

        found.balance.amount shouldBe BigDecimal(3_000)
    }

    @Test
    fun `잔액을 갱신해도 기존 memberId와 개인 한도 예외가 보존된다`() {
        policyAdapter.save(
            policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000))),
            appliedAt = LocalDateTime.now().minusDays(1),
            createdByAdminId = "admin-01",
        )
        val walletId: String = UUID.randomUUID().toString()
        entityManager.persist(
            PointWalletEntity(id = walletId, memberId = "member-4", balance = 1_000L, holdingLimitOverride = 9_000_000L),
        )
        entityManager.flush()
        entityManager.clear()

        val toUpdate: PointWallet = adapter.findByIdForUpdate(walletId)
        toUpdate.decrease(PointAmount(BigDecimal(1_000)))
        adapter.updateBalance(toUpdate)
        entityManager.flush()
        entityManager.clear()

        val found: PointWallet = adapter.findByMemberId("member-4")!!

        found.balance.amount shouldBe BigDecimal.ZERO
        found.holdingLimit.value shouldBe BigDecimal(9_000_000L)
    }

    @Test
    fun `존재하지 않는 지갑을 잠금 조회하면 예외가 발생한다`() {
        shouldThrow<PointDomainException> { adapter.findByIdForUpdate("no-such-wallet") }
    }
}
