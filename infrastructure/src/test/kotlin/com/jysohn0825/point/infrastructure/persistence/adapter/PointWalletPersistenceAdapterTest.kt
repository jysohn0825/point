package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.maxHoldingAmount
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
import java.util.UUID

@DataJpaTest
@Import(PointWalletPersistenceAdapter::class, PointPolicyPersistenceAdapter::class, FakeCacheExecutor::class)
class PointWalletPersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointWalletPersistenceAdapter,
    @Autowired private val policyAdapter: PointPolicyPersistenceAdapter,
) : BehaviorSpec({
        Given("존재하지 않는 회원일 때") {
            When("잠금 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { adapter.findByMemberIdForUpdate("no-such-member") }
                }
            }
        }

        Given("정책이 적용되어 있는 회원의 지갑이 있을 때") {
            When("지갑을 조회하면") {
                Then("정책의 보유한도가 적용된다") {
                    policyAdapter.save(
                        policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(2_000_000))),
                        appliedAt = LocalDate.now().minusDays(1),
                        createdByAdminId = "admin-01",
                    )
                    entityManager.persist(
                        PointWalletEntity(id = UUID.randomUUID().toString(), memberId = "member-1", balance = BigDecimal(1_000)),
                    )
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointWallet = adapter.findByMemberIdForUpdate("member-1")

                    found.balance.amount shouldBe BigDecimal(1_000L)
                    found.holdingLimit.value shouldBe BigDecimal(2_000_000)
                }
            }
        }

        Given("존재하지 않는 회원일 때") {
            When("findByMemberId로 조회하면") {
                Then("null이 반환된다") {
                    adapter.findByMemberId("unknown") shouldBe null
                }
            }
        }

        Given("정책이 적용되어 있을 때") {
            When("신규 지갑을 저장하면") {
                Then("잔액이 저장된다") {
                    policyAdapter.save(
                        policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000))),
                        appliedAt = LocalDate.now().minusDays(1),
                        createdByAdminId = "admin-01",
                    )
                    val wallet: PointWallet = pointWallet(balance = balance(BigDecimal(3_000)))

                    adapter.save(wallet = wallet, memberId = "member-3")
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointWallet = adapter.findByMemberIdForUpdate("member-3")

                    found.balance.amount shouldBe BigDecimal(3_000)
                }
            }
        }
    })
