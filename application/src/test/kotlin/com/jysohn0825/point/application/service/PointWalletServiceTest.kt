package com.jysohn0825.point.application.service

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.FakePointPolicyRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointWalletServiceTest :
    BehaviorSpec({
        Given("회원의 지갑이 존재할 때") {
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = pointWallet(balance = Balance(BigDecimal(1_400))))
            val policyRepository: FakePointPolicyRepository = FakePointPolicyRepository()
            val sut: PointWalletService = PointWalletService(walletRepository, policyRepository)

            When("지갑을 조회하면") {
                val wallet: PointWallet = sut.getWallet("member-1").pointWallet

                Then("잔액이 그대로 반환된다") {
                    wallet.balance.amount shouldBe BigDecimal(1_400)
                }
            }
        }

        Given("회원의 지갑이 존재하지 않을 때") {
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            val policyRepository: FakePointPolicyRepository = FakePointPolicyRepository()
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
                    walletRepository.findByMemberId("member-new") shouldBe wallet
                }
            }
        }

        Given("이미 지갑을 보유한 회원일 때") {
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = pointWallet())
            val policyRepository: FakePointPolicyRepository = FakePointPolicyRepository()
            policyRepository.reset(policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(value = BigDecimal(500_000))))
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
