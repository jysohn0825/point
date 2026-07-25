package com.jysohn0825.point.application.usage

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.grantedBy
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.assertions.throwables.shouldThrow
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
    ) = Unit

    override fun findById(earningId: String): PointEarning = earnings.first { it.id == earningId }

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? = earnings.firstOrNull { it.earnType == earnType && it.sourceReferenceId == sourceReferenceId }

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        earnings.filter { it.canBeRedeemed() }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earnings.filter { it.id in earningIds }

    override fun findAllByWalletId(walletId: String): List<PointEarning> = earnings.toList()

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> = emptyList()

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> = emptyList()

    private fun PointEarning.canBeRedeemed(): Boolean =
        status.isActive() && remainingAmount.value.signum() > 0 && !isExpiredAt(LocalDateTime.now())
}

private class FakePointUsageRepository(
    initial: PointUsage? = null,
) : PointUsageRepository {
    var usage: PointUsage? = initial
    var lastSaveCancellationCall: SaveCancellationCall? = null

    data class SaveCancellationCall(
        val requestedLines: List<CancellationLine>,
        val reearnedEarningIds: List<String?>,
    )

    override fun save(
        usage: PointUsage,
        walletId: String,
    ) {
        this.usage = usage
    }

    override fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        reearnedEarningIds: List<String?>,
        canceledAt: LocalDateTime,
    ) {
        this.usage = usage
        lastSaveCancellationCall = SaveCancellationCall(requestedLines, reearnedEarningIds)
    }

    override fun findById(usageId: String): PointUsage = requireNotNull(usage) { "usage not found: $usageId" }

    override fun findLinesByEarningId(earningId: String): List<EarningUsageTrace> = emptyList()

    override fun findAllByWalletId(walletId: String): List<PointUsage> = listOfNotNull(usage)
}

private class FakePointPolicyRepository(
    var policy: PointPolicy,
) : PointPolicyRepository {
    override fun getCurrent(): PointPolicy = policy

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) {
        this.policy = policy
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository = FakePointEarningRepository(),
    usageRepository: FakePointUsageRepository = FakePointUsageRepository(),
    policyRepository: FakePointPolicyRepository = FakePointPolicyRepository(pointPolicy()),
): UsePointService =
    UsePointService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        usageRepository = usageRepository,
        policyRepository = policyRepository,
    )

class UsePointServiceTest :
    BehaviorSpec({
        Given("수기지급 적립건과 일반 적립건이 함께 있을 때") {
            val manualEarning =
                pointEarning(
                    id = "manual-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnType = EarnType.MANUAL,
                    grantedBy = grantedBy(),
                    earnedAt = LocalDateTime.now().minusDays(1),
                )
            val systemEarning =
                pointEarning(
                    id = "system-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now().minusDays(2),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(2_000))))
            val earningRepository = FakePointEarningRepository(listOf(systemEarning, manualEarning))
            val usePointService = service(walletRepository, earningRepository)

            When("일반 적립건보다 먼저 만료되지 않더라도 수기지급건을 사용하면") {
                val usage =
                    usePointService.use(
                        UsePointDto(memberId = "member-1", orderNumber = "ORDER-1", amount = BigDecimal(500)),
                    )

                Then("수기지급 적립건이 먼저 차감된다") {
                    usage.lines.size shouldBe 1
                    usage.lines[0].earningId shouldBe "manual-1"
                    manualEarning.remainingAmount.value shouldBe BigDecimal(500)
                    systemEarning.remainingAmount.value shouldBe BigDecimal(1_000)
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_500)
                }
            }
        }

        Given("일반 적립건 중 만료일이 다른 두 건이 있을 때") {
            val soonExpiring =
                pointEarning(
                    id = "soon",
                    amount = pointAmount(BigDecimal(500)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(1),
                )
            val laterExpiring =
                pointEarning(
                    id = "later",
                    amount = pointAmount(BigDecimal(500)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(30),
                )
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository = FakePointEarningRepository(listOf(laterExpiring, soonExpiring))
            val usePointService = service(walletRepository, earningRepository)

            When("포인트를 사용하면") {
                usePointService.use(UsePointDto(memberId = "member-1", orderNumber = "ORDER-2", amount = BigDecimal(500)))

                Then("만료 임박한 적립건이 먼저 소진된다") {
                    soonExpiring.remainingAmount.value shouldBe BigDecimal.ZERO
                    laterExpiring.remainingAmount.value shouldBe BigDecimal(500)
                }
            }
        }

        Given("가용 포인트보다 많은 금액을 사용하려고 하면") {
            val earning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(500)))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(500))))
            val earningRepository = FakePointEarningRepository(listOf(earning))
            val usePointService = service(walletRepository, earningRepository)

            When("사용을 시도하면") {
                Then("예외가 발생하고 잔액은 변하지 않는다") {
                    shouldThrow<PointBusinessException> {
                        usePointService.use(UsePointDto(memberId = "member-1", orderNumber = "ORDER-3", amount = BigDecimal(600)))
                    }
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(500)
                }
            }
        }

        Given("만료되지 않은 적립건에서 차감된 사용 건을 전액 취소하면") {
            val earning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(300))
            val usage = pointUsage(lines = listOf(UsageLine("e1", BigDecimal(300))))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(700))))
            val earningRepository = FakePointEarningRepository(listOf(earning))
            val usageRepository = FakePointUsageRepository(usage)
            val usePointService = service(walletRepository, earningRepository, usageRepository)

            When("사용취소를 실행하면") {
                val result = usePointService.cancelUsage(CancelUsagePointDto(memberId = "member-1", usageId = usage.id))

                Then("원 적립건이 그대로 복원되고 신규 적립은 생기지 않는다") {
                    result.reEarnings.shouldBeEmpty()
                    result.requestedLines.size shouldBe 1
                    result.requestedLines[0].restorationType shouldBe RestorationType.RESTORED
                    earning.remainingAmount.value shouldBe BigDecimal(1_000)
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                    result.usage.status shouldBe UsageStatus.FULLY_CANCELED
                }
            }
        }

        Given("이미 만료된 적립건에서 차감된 사용 건을 취소하면") {
            val expiredEarning =
                pointEarning(
                    id = "expired-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            expiredEarning.use(BigDecimal(400))
            val usage = pointUsage(lines = listOf(UsageLine("expired-1", BigDecimal(400))))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(600))))
            val earningRepository = FakePointEarningRepository(listOf(expiredEarning))
            val usageRepository = FakePointUsageRepository(usage)
            val usePointService = service(walletRepository, earningRepository, usageRepository)

            When("사용취소를 실행하면") {
                val result = usePointService.cancelUsage(CancelUsagePointDto(memberId = "member-1", usageId = usage.id))

                Then("만료된 적립건은 복원되지 않고 신규 적립으로 대체된다") {
                    result.reEarnings.size shouldBe 1
                    result.reEarnings[0].amount.value shouldBe BigDecimal(400)
                    result.requestedLines[0].restorationType shouldBe RestorationType.RE_EARNED
                    expiredEarning.remainingAmount.value shouldBe BigDecimal(600)
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("사용 건의 일부만 취소하면") {
            val earning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(500))
            val usage = pointUsage(lines = listOf(UsageLine("e1", BigDecimal(500))))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(500))))
            val earningRepository = FakePointEarningRepository(listOf(earning))
            val usageRepository = FakePointUsageRepository(usage)
            val usePointService = service(walletRepository, earningRepository, usageRepository)

            When("일부 금액만 취소하면") {
                val result =
                    usePointService.cancelUsage(
                        CancelUsagePointDto(memberId = "member-1", usageId = usage.id, amount = BigDecimal(200)),
                    )

                Then("사용 건은 부분취소 상태가 되고 남은 사용 금액이 갱신된다") {
                    result.usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                    result.usage.remainingAmount shouldBe BigDecimal(300)
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(700)
                }
            }
        }

        Given("회원이 주문에서 포인트를 사용한 이력이 있을 때") {
            val earning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val walletRepository = FakePointWalletRepository(pointWallet(balance = Balance(BigDecimal(1_000))))
            val earningRepository = FakePointEarningRepository(listOf(earning))
            val usePointService = service(walletRepository, earningRepository)
            val usage = usePointService.use(UsePointDto(memberId = "member-1", orderNumber = "ORDER-9", amount = BigDecimal(300)))

            When("사용건 목록을 조회하면") {
                val usages = usePointService.getUsages("member-1")

                Then("사용건이 반환된다") {
                    usages.size shouldBe 1
                    usages[0].id shouldBe usage.id
                }
            }

            When("사용건 상세를 조회하면") {
                val found = usePointService.getUsage(usage.id)

                Then("동일한 사용건이 반환된다") {
                    found.id shouldBe usage.id
                    found.orderNumber shouldBe usage.orderNumber
                }
            }
        }
    })
