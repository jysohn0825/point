package com.jysohn0825.point.application.expiration

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

private class FakePointWalletRepository(
    var wallet: PointWallet,
) : PointWalletRepository {
    override fun findByMemberIdForUpdate(memberId: String): PointWallet = wallet

    override fun findByMemberId(memberId: String): PointWallet = wallet

    override fun findByIdForUpdate(walletId: String): PointWallet = wallet

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        this.wallet = wallet
    }

    override fun updateBalance(wallet: PointWallet) {
        this.wallet = wallet
    }
}

private class FakePointEarningRepository(
    initial: List<PointEarning> = emptyList(),
) : PointEarningRepository {
    val earnings = initial.toMutableList()
    var updateStatusAllCallCount = 0

    override fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    ) {
        earnings.add(earning)
    }

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) {
        this.earnings.addAll(earnings)
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) = Unit

    override fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    ) {
        updateStatusAllCallCount++
    }

    override fun findById(earningId: String): PointEarning = earnings.first { it.id == earningId }

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        earnings.filter { it.status.isActive() && it.remainingAmount.value.signum() > 0 && !it.isExpiredAt(LocalDateTime.now()) }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earnings.filter { it.id in earningIds }

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? = earnings.firstOrNull { it.earnType == earnType && it.sourceReferenceId == sourceReferenceId }

    override fun findAllByWalletId(walletId: String): List<PointEarning> = earnings.toList()

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> =
        if (earnings.any { it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now) }) listOf(WALLET_ID) else emptyList()

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> = earnings.filter { it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now) }

    companion object {
        const val WALLET_ID = "1"
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository,
): PointEarningExpirationService =
    PointEarningExpirationService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
    )

class PointEarningExpirationServiceTest :
    BehaviorSpec({
        Given("이미 만료일이 지난 ACTIVE 적립건이 있을 때") {
            val expired =
                pointEarning(
                    id = "expired-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository = FakePointEarningRepository(listOf(expired))
            val expirationService = service(walletRepository, earningRepository)

            When("walletId 기준으로 만료 처리를 실행하면") {
                val result = expirationService.expireWalletEarnings("1")

                Then("적립건이 EXPIRED 상태가 되고 지갑 잔액이 그만큼 감소한다") {
                    result.size shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                    earningRepository.updateStatusAllCallCount shouldBe 1
                }
            }
        }

        Given("이미 사용되어 EXHAUSTED 상태인 적립건만 있을 때") {
            val exhausted =
                pointEarning(
                    id = "exhausted-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            exhausted.use(BigDecimal(1_000))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal.ZERO)))
            val earningRepository = FakePointEarningRepository(listOf(exhausted))
            val expirationService = service(walletRepository, earningRepository)

            When("만료 처리를 실행하면") {
                val result = expirationService.expireWalletEarnings("1")

                Then("잔여금액이 없으므로 만료 대상에서 제외되고 아무 것도 변하지 않는다") {
                    result.shouldBeEmpty()
                    exhausted.status shouldBe EarningStatus.EXHAUSTED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("만료 대상 적립건이 없을 때") {
            val active =
                pointEarning(
                    id = "active-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(365),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository = FakePointEarningRepository(listOf(active))
            val expirationService = service(walletRepository, earningRepository)

            When("만료 처리를 실행하면") {
                val result = expirationService.expireWalletEarnings("1")

                Then("아무 것도 처리하지 않는다") {
                    result.shouldBeEmpty()
                    active.status shouldBe EarningStatus.ACTIVE
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("아직 만료일이 지나지 않은 ACTIVE 적립건이 있을 때") {
            val notYetDue =
                pointEarning(
                    id = "not-due-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(365),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository = FakePointEarningRepository(listOf(notYetDue))
            val expirationService = service(walletRepository, earningRepository)

            When("강제 만료를 실행하면") {
                val result = expirationService.forceExpireEarning("member-1", "not-due-1")

                Then("만료일과 무관하게 즉시 EXPIRED 처리되고 지갑 잔액이 감소한다") {
                    result.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("관리자가 특정 회원의 만료를 즉시 트리거할 때") {
            val expired =
                pointEarning(
                    id = "expired-2",
                    amount = pointAmount(BigDecimal(700)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(700))))
            val earningRepository = FakePointEarningRepository(listOf(expired))
            val expirationService = service(walletRepository, earningRepository)

            When("expireMemberEarningsNow를 호출하면") {
                val result = expirationService.expireMemberEarningsNow("member-1")

                Then("해당 회원의 만료 대상 적립건이 즉시 처리된다") {
                    result.size shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }
    })
