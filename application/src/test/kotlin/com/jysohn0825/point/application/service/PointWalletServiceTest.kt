package com.jysohn0825.point.application.service

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

private class FakeWalletServiceRepository(
    private var wallet: PointWallet?,
) : PointWalletRepository {
    var savedWallet: PointWallet? = null
    var savedMemberId: String? = null

    override fun findByMemberIdForUpdate(memberId: String): PointWallet = requireNotNull(wallet)

    override fun findByMemberId(memberId: String): PointWallet? = wallet

    override fun findByIdForUpdate(walletId: String): PointWallet = requireNotNull(wallet)

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        this.wallet = wallet
        this.savedWallet = wallet
        this.savedMemberId = memberId
    }

    override fun updateBalance(wallet: PointWallet) {
        this.wallet = wallet
    }
}

private class FakeWalletServicePolicyRepository(
    private val policy: PointPolicy,
) : PointPolicyRepository {
    override fun getCurrent(): PointPolicy = policy

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) = Unit
}

class PointWalletServiceTest :
    BehaviorSpec({
        Given("회원의 지갑이 존재할 때") {
            val walletRepository: FakeWalletServiceRepository =
                FakeWalletServiceRepository(pointWallet(balance = Balance(BigDecimal(1_400))))
            val policyRepository: FakeWalletServicePolicyRepository = FakeWalletServicePolicyRepository(pointPolicy())
            val sut: PointWalletService = PointWalletService(walletRepository, policyRepository)

            When("지갑을 조회하면") {
                val wallet: PointWallet = sut.getWallet("member-1").pointWallet

                Then("잔액이 그대로 반환된다") {
                    wallet.balance.amount shouldBe BigDecimal(1_400)
                }
            }
        }

        Given("회원의 지갑이 존재하지 않을 때") {
            val walletRepository: FakeWalletServiceRepository = FakeWalletServiceRepository(null)
            val policyRepository: FakeWalletServicePolicyRepository = FakeWalletServicePolicyRepository(pointPolicy())
            val sut: PointWalletService = PointWalletService(walletRepository, policyRepository)

            When("지갑을 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        sut.getWallet("member-unknown")
                    }
                }
            }

            When("지갑을 생성하면") {
                val wallet: PointWallet = sut.createWallet("member-new").pointWallet

                Then("현재 정책의 보유한도로 잔액 0인 지갑이 생성된다") {
                    wallet.balance.amount shouldBe BigDecimal.ZERO
                    wallet.holdingLimit.value shouldBe pointPolicy().maxHoldingAmount.value
                }

                Then("회원 식별자와 함께 저장된다") {
                    walletRepository.savedWallet shouldBe wallet
                    walletRepository.savedMemberId shouldBe "member-new"
                }
            }
        }

        Given("이미 지갑을 보유한 회원일 때") {
            val walletRepository: FakeWalletServiceRepository = FakeWalletServiceRepository(pointWallet())
            val policyRepository: FakeWalletServicePolicyRepository =
                FakeWalletServicePolicyRepository(pointPolicy(maxHoldingAmount = maxHoldingAmount(value = BigDecimal(500_000))))
            val sut: PointWalletService = PointWalletService(walletRepository, policyRepository)

            When("지갑을 다시 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        sut.createWallet("member-1")
                    }
                }
            }
        }
    })
