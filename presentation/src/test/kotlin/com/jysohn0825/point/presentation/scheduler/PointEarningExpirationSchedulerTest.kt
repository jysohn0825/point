package com.jysohn0825.point.presentation.scheduler

import com.jysohn0825.point.application.service.PointEarningExpirationService
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

private class FakeSchedulerWalletRepository(
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

private class FakeSchedulerEarningRepository(
    initial: List<PointEarning>,
) : PointEarningRepository {
    val earnings: MutableList<PointEarning> = initial.toMutableList()
    var updateStatusAllCallCount: Int = 0

    override fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    ) = Unit

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) = Unit

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

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? = null

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> = emptyList()

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earnings.filter { it.id in earningIds }

    override fun findAllByWalletId(walletId: String): List<PointEarning> = earnings.toList()

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> =
        if (earnings.any { it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now) }) {
            listOf(WALLET_ID)
        } else {
            emptyList()
        }

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> = earnings.filter { it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now) }

    companion object {
        const val WALLET_ID = "1"
    }
}

class PointEarningExpirationSchedulerTest :
    BehaviorSpec({
        Given("만료 대상 적립건이 있는 지갑이 있을 때") {
            val expired: PointEarning =
                pointEarning(
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository = FakeSchedulerWalletRepository(pointWallet(id = "1", balance = balance(BigDecimal(1_000))))
            val earningRepository = FakeSchedulerEarningRepository(listOf(expired))
            val scheduler =
                PointEarningExpirationScheduler(
                    PointEarningExpirationService(walletRepository = walletRepository, earningRepository = earningRepository),
                )

            When("스케줄러가 실행되면") {
                scheduler.expireDueEarnings()

                Then("만료 대상 적립건이 만료 처리된다") {
                    expired.status shouldBe EarningStatus.EXPIRED
                    earningRepository.updateStatusAllCallCount shouldBe 1
                }
            }
        }

        Given("만료 대상 적립건이 없을 때") {
            val active: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)), period = expirationPeriod(365))
            val walletRepository = FakeSchedulerWalletRepository(pointWallet(id = "1"))
            val earningRepository = FakeSchedulerEarningRepository(listOf(active))
            val scheduler =
                PointEarningExpirationScheduler(
                    PointEarningExpirationService(walletRepository = walletRepository, earningRepository = earningRepository),
                )

            When("스케줄러가 실행되면") {
                scheduler.expireDueEarnings()

                Then("아무 것도 처리되지 않는다") {
                    active.status shouldBe EarningStatus.ACTIVE
                    earningRepository.updateStatusAllCallCount shouldBe 0
                }
            }
        }
    })
