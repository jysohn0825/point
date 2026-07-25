package com.jysohn0825.point.application.service

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

private class FakeExpirationWalletRepository(
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

private class FakeExpirationEarningRepository(
    initial: List<PointEarning> = emptyList(),
) : PointEarningRepository {
    val earnings: MutableList<PointEarning> = initial.toMutableList()
    var updateStatusAllCallCount: Int = 0

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
        if (earnings.any {
                it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now)
            }
        ) {
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

private fun service(
    walletRepository: FakeExpirationWalletRepository,
    earningRepository: FakeExpirationEarningRepository,
): PointEarningExpirationService =
    PointEarningExpirationService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
    )

class PointEarningExpirationServiceTest :
    BehaviorSpec({
        Given("이미 만료일이 지난 ACTIVE 적립건이 있을 때") {
            val expired: PointEarning =
                pointEarning(
                    id = "expired-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(expired))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("walletId 기준으로 만료 처리를 실행하면") {
                val result: List<PointEarning> = expirationService.expireWalletEarnings("1")

                Then("적립건이 EXPIRED 상태가 되고 지갑 잔액이 그만큼 감소한다") {
                    result.size shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                    earningRepository.updateStatusAllCallCount shouldBe 1
                }
            }
        }

        Given("이미 사용되어 EXHAUSTED 상태인 적립건만 있을 때") {
            val exhausted: PointEarning =
                pointEarning(
                    id = "exhausted-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            exhausted.use(BigDecimal(1_000))
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal.ZERO)))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(exhausted))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("만료 처리를 실행하면") {
                val result: List<PointEarning> = expirationService.expireWalletEarnings("1")

                Then("잔여금액이 없으므로 만료 대상에서 제외되고 아무 것도 변하지 않는다") {
                    result.shouldBeEmpty()
                    exhausted.status shouldBe EarningStatus.EXHAUSTED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("만료 대상 적립건이 없을 때") {
            val active: PointEarning =
                pointEarning(
                    id = "active-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(365),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(active))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("만료 처리를 실행하면") {
                val result: List<PointEarning> = expirationService.expireWalletEarnings("1")

                Then("아무 것도 처리하지 않는다") {
                    result.shouldBeEmpty()
                    active.status shouldBe EarningStatus.ACTIVE
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("만료 대상 지갑이 있을 때") {
            val expired: PointEarning =
                pointEarning(
                    id = "expired-batch-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(expired))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("배치로 전체 만료 대상을 처리하면") {
                val processedWalletCount: Int = expirationService.expireAllDue()

                Then("만료 대상 지갑 수만큼 처리되고 적립건이 만료된다") {
                    processedWalletCount shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("만료 대상 지갑이 없을 때") {
            val active: PointEarning =
                pointEarning(
                    id = "active-batch-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(365),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(active))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("배치로 전체 만료 대상을 처리하면") {
                val processedWalletCount: Int = expirationService.expireAllDue()

                Then("처리된 지갑이 0건이다") {
                    processedWalletCount shouldBe 0
                    active.status shouldBe EarningStatus.ACTIVE
                }
            }
        }

        Given("관리자가 특정 회원의 만료를 트리거했지만 만료 대상이 없을 때") {
            val active: PointEarning =
                pointEarning(
                    id = "active-2",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(365),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(active))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("expireMemberEarningsNow를 호출하면") {
                val result: List<PointEarning> = expirationService.expireMemberEarningsNow("member-1")

                Then("아무 것도 처리하지 않는다") {
                    result.shouldBeEmpty()
                    active.status shouldBe EarningStatus.ACTIVE
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("관리자가 특정 회원의 만료를 즉시 트리거할 때") {
            val expired: PointEarning =
                pointEarning(
                    id = "expired-2",
                    amount = pointAmount(BigDecimal(700)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            val walletRepository: FakeExpirationWalletRepository =
                FakeExpirationWalletRepository(pointWallet(balance = Balance(BigDecimal(700))))
            val earningRepository: FakeExpirationEarningRepository = FakeExpirationEarningRepository(listOf(expired))
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("expireMemberEarningsNow를 호출하면") {
                val result: List<PointEarning> = expirationService.expireMemberEarningsNow("member-1")

                Then("해당 회원의 만료 대상 적립건이 즉시 처리된다") {
                    result.size shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }
    })
