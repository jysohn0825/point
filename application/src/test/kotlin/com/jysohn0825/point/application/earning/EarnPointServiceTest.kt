package com.jysohn0825.point.application.earning

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
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

private class FakePointEarningRepository : PointEarningRepository {
    val earnings = mutableListOf<PointEarning>()
    private val walletIdByEarningId = mutableMapOf<String, String>()

    override fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    ) {
        earnings.add(earning)
        walletIdByEarningId[earning.id] = walletId
    }

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) {
        this.earnings.addAll(earnings)
        earnings.forEach { walletIdByEarningId[it.id] = walletId }
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) {
        walletIdByEarningId[earning.id] = walletId
    }

    override fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    ) {
        earnings.forEach { walletIdByEarningId[it.id] = walletId }
    }

    override fun findById(earningId: String): PointEarning = earnings.first { it.id == earningId }

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> = earnings.filter { walletIdByEarningId[it.id] == walletId }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earnings.filter { it.id in earningIds }

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? =
        earnings.firstOrNull {
            walletIdByEarningId[it.id] == walletId && it.earnType == earnType && it.sourceReferenceId == sourceReferenceId
        }

    override fun findAllByWalletId(walletId: String): List<PointEarning> = earnings.filter { walletIdByEarningId[it.id] == walletId }

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> =
        earnings
            .filter { it.status.isActive() && it.remainingAmount.value.signum() > 0 && it.isExpiredAt(now) }
            .mapNotNull { walletIdByEarningId[it.id] }
            .distinct()

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> =
        earnings.filter {
            walletIdByEarningId[it.id] == walletId &&
                it.status.isActive() &&
                it.remainingAmount.value.signum() > 0 &&
                it.isExpiredAt(now)
        }
}

private class FakePointUsageRepository : PointUsageRepository {
    val usages = mutableListOf<PointUsage>()
    private val walletIdByUsageId = mutableMapOf<String, String>()

    override fun save(
        usage: PointUsage,
        walletId: String,
    ) {
        usages.add(usage)
        walletIdByUsageId[usage.id] = walletId
    }

    override fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        reearnedEarningIds: List<String?>,
        canceledAt: LocalDateTime,
    ) {
        walletIdByUsageId[usage.id] = walletId
    }

    override fun findById(usageId: String): PointUsage = usages.first { it.id == usageId }

    override fun findLinesByEarningId(earningId: String): List<EarningUsageTrace> =
        usages.flatMap { usage ->
            usage.lines.filter { it.earningId == earningId }.map { line ->
                EarningUsageTrace(orderNumber = usage.orderNumber, amount = line.amount)
            }
        }

    override fun findAllByWalletId(walletId: String): List<PointUsage> = usages.filter { walletIdByUsageId[it.id] == walletId }
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
    policyRepository: FakePointPolicyRepository = FakePointPolicyRepository(pointPolicy()),
    usageRepository: FakePointUsageRepository = FakePointUsageRepository(),
): EarnPointService =
    EarnPointService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        policyRepository = policyRepository,
        usageRepository = usageRepository,
    )

class EarnPointServiceTest :
    BehaviorSpec({
        Given("처음 보는 sourceReferenceId로 적립을 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)

            When("적립을 실행하면") {
                val earning =
                    earnPointService.earn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-1",
                        ),
                    )

                Then("새 적립건이 생성되고 지갑 잔액이 증가한다") {
                    earning.amount.value shouldBe BigDecimal(1_000)
                    earning.sourceReferenceId shouldBe "ORDER-1"
                    earningRepository.earnings.size shouldBe 1
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("이미 처리된 sourceReferenceId로 동일한 적립을 다시 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)
            val dto =
                EarnPointDto(
                    memberId = "member-1",
                    amount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM,
                    sourceReferenceId = "ORDER-1",
                )

            val firstEarning = earnPointService.earn(dto)

            When("같은 요청을 재시도하면") {
                val secondEarning = earnPointService.earn(dto)

                Then("새 적립건을 만들지 않고 기존 적립건을 그대로 반환한다") {
                    secondEarning.id shouldBe firstEarning.id
                    earningRepository.earnings.size shouldBe 1
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("1회 적립 한도를 초과하는 금액으로 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val policyRepository = FakePointPolicyRepository(pointPolicy(maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(10_000))))
            val earnPointService = service(walletRepository, policyRepository = policyRepository)

            When("적립을 시도하면") {
                Then("예외가 발생하고 잔액은 변하지 않는다") {
                    shouldThrow<PointBusinessException> {
                        earnPointService.earn(
                            EarnPointDto(
                                memberId = "member-1",
                                amount = BigDecimal(10_001),
                                earnType = EarnType.SYSTEM,
                                sourceReferenceId = "ORDER-2",
                            ),
                        )
                    }
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("사용되지 않은 적립건을 취소하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)
            val earning =
                earnPointService.earn(
                    EarnPointDto(
                        memberId = "member-1",
                        amount = BigDecimal(1_000),
                        earnType = EarnType.SYSTEM,
                        sourceReferenceId = "ORDER-3",
                    ),
                )

            When("적립 취소를 실행하면") {
                val canceled = earnPointService.cancelEarning("member-1", earning.id)

                Then("적립건이 취소 상태가 되고 지갑 잔액이 원복된다") {
                    canceled.status shouldBe EarningStatus.CANCELED
                    canceled.remainingAmount.value shouldBe BigDecimal.ZERO
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("일부 사용된 적립건을 취소하려고 하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)
            val earning =
                earnPointService.earn(
                    EarnPointDto(
                        memberId = "member-1",
                        amount = BigDecimal(1_000),
                        earnType = EarnType.SYSTEM,
                        sourceReferenceId = "ORDER-4",
                    ),
                )
            earning.use(BigDecimal(400))

            When("적립 취소를 시도하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalStateException> {
                        earnPointService.cancelEarning("member-1", earning.id)
                    }
                }
            }
        }

        Given("적립건이 여러 개 있을 때") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)
            earnPointService.earn(
                EarnPointDto(memberId = "member-1", amount = BigDecimal(1_000), earnType = EarnType.SYSTEM, sourceReferenceId = "ORDER-5"),
            )
            val second =
                earnPointService.earn(
                    EarnPointDto(memberId = "member-1", amount = BigDecimal(500), earnType = EarnType.SYSTEM, sourceReferenceId = "ORDER-6"),
                )

            When("목록을 조회하면") {
                val earnings = earnPointService.getEarnings("member-1")

                Then("회원의 전체 적립건이 반환된다") {
                    earnings.size shouldBe 2
                }
            }

            When("상세를 조회하면") {
                val earning = earnPointService.getEarning(second.id)

                Then("해당 적립건이 반환된다") {
                    earning.id shouldBe second.id
                    earning.amount.value shouldBe BigDecimal(500)
                }
            }
        }

        Given("적립건이 특정 주문에서 사용된 이력이 있을 때") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val usageRepository = FakePointUsageRepository()
            val earnPointService = service(walletRepository, earningRepository, usageRepository = usageRepository)
            val earning =
                earnPointService.earn(
                    EarnPointDto(memberId = "member-1", amount = BigDecimal(1_000), earnType = EarnType.SYSTEM, sourceReferenceId = "ORDER-7"),
                )
            earning.use(BigDecimal(300))
            usageRepository.save(
                PointUsage.use(
                    orderNumber = OrderNumber("A1234"),
                    lines = listOf(UsageLine(earning.id, BigDecimal(300))),
                ),
                walletId = walletRepository.wallet.id,
            )

            When("사용 추적을 조회하면") {
                val traces = earnPointService.getUsageTraces(earning.id)

                Then("차감된 주문번호와 금액이 반환된다") {
                    traces.size shouldBe 1
                    traces[0].orderNumber.value shouldBe "A1234"
                    traces[0].amount shouldBe BigDecimal(300)
                }
            }
        }
    })
