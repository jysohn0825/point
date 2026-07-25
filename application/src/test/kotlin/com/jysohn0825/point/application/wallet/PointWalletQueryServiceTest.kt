package com.jysohn0825.point.application.wallet

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

private class FakePointWalletRepository(
    private val wallet: PointWallet?,
) : PointWalletRepository {
    override fun findByMemberIdForUpdate(memberId: String): PointWallet = requireNotNull(wallet)

    override fun findByMemberId(memberId: String): PointWallet? = wallet

    override fun findByIdForUpdate(walletId: String): PointWallet = requireNotNull(wallet)

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) = Unit

    override fun updateBalance(wallet: PointWallet) = Unit
}

class PointWalletQueryServiceTest :
    BehaviorSpec({
        Given("회원의 지갑이 존재할 때") {
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_400))))
            val walletQueryService: PointWalletQueryService = PointWalletQueryService(walletRepository)

            When("지갑을 조회하면") {
                val wallet: PointWallet = walletQueryService.getWallet("member-1")

                Then("잔액이 그대로 반환된다") {
                    wallet.balance.amount shouldBe BigDecimal(1_400)
                }
            }
        }

        Given("회원의 지갑이 존재하지 않을 때") {
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository(null)
            val walletQueryService: PointWalletQueryService = PointWalletQueryService(walletRepository)

            When("지갑을 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        walletQueryService.getWallet("member-unknown")
                    }
                }
            }
        }
    })
