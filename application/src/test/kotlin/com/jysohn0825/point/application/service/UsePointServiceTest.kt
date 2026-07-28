package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.CancelUsagePointDto
import com.jysohn0825.point.application.service.dto.CancelUsagePointResultDto
import com.jysohn0825.point.application.service.dto.PointUsageResultDto
import com.jysohn0825.point.application.service.dto.UsePointDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.event.PointsUsageCancelled
import com.jysohn0825.point.domain.event.PointsUsed
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointPolicyRepository
import com.jysohn0825.point.domain.repository.FakePointUsageRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.service.DefaultPointCancellationAllocator
import com.jysohn0825.point.domain.service.DefaultPointRedemptionAllocator
import com.jysohn0825.point.domain.service.PointCancellationAllocator
import com.jysohn0825.point.domain.service.PointRedemptionAllocator
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.grantedBy
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

private class FakeUseKeyGenerator : DistributedKeyGenerator {
    private val counter: AtomicLong = AtomicLong()

    override fun next(name: String): Long = counter.incrementAndGet()
}

private class FakeUseEventPublisher : ApplicationEventPublisher {
    val publishedEvents: MutableList<Any> = mutableListOf()

    override fun publishEvent(event: Any) {
        publishedEvents.add(event)
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository = FakePointEarningRepository(),
    usageRepository: FakePointUsageRepository = FakePointUsageRepository(),
    policyRepository: FakePointPolicyRepository = FakePointPolicyRepository(),
    keyGenerator: DistributedKeyGenerator = FakeUseKeyGenerator(),
    eventPublisher: ApplicationEventPublisher = FakeUseEventPublisher(),
    redemptionAllocator: PointRedemptionAllocator = DefaultPointRedemptionAllocator(),
    cancellationAllocator: PointCancellationAllocator = DefaultPointCancellationAllocator(),
): UsePointService =
    UsePointService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        usageRepository = usageRepository,
        policyRepository = policyRepository,
        keyGenerator = keyGenerator,
        eventPublisher = eventPublisher,
        redemptionAllocator = redemptionAllocator,
        cancellationAllocator = cancellationAllocator,
    )

class UsePointServiceTest :
    BehaviorSpec({
        Given("수기지급 적립건과 일반 적립건이 함께 있을 때") {
            val manualEarning: PointEarning =
                pointEarning(
                    id = "manual-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnType = EarnType.MANUAL,
                    grantedBy = grantedBy(),
                    earnedAt = LocalDateTime.now().minusDays(1),
                )
            val systemEarning: PointEarning =
                pointEarning(
                    id = "system-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now().minusDays(2),
                )
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(2_000)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = systemEarning, walletId = wallet.id, policyId = "policy-1")
            earningRepository.save(earning = manualEarning, walletId = wallet.id, policyId = "policy-1")
            val usePointService: UsePointService = service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("일반 적립건보다 먼저 만료되지 않더라도 수기지급건을 사용하면") {
                val usage: PointUsage =
                    usePointService
                        .use(
                            UsePointDto(memberId = "member-1", orderNumber = "ORDER-1", amount = BigDecimal(500)),
                        ).pointUsage

                Then("수기지급 적립건이 먼저 차감된다") {
                    usage.lines.size shouldBe 1
                    usage.lines[0].earningId shouldBe "manual-1"
                    manualEarning.remainingAmount.value shouldBe BigDecimal(500)
                    systemEarning.remainingAmount.value shouldBe BigDecimal(1_000)
                    wallet.balance.amount shouldBe BigDecimal(1_500)
                }
            }
        }

        Given("일반 적립건 중 만료일이 다른 두 건이 있을 때") {
            val soonExpiring: PointEarning =
                pointEarning(
                    id = "soon",
                    amount = pointAmount(BigDecimal(500)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(1),
                )
            val laterExpiring: PointEarning =
                pointEarning(
                    id = "later",
                    amount = pointAmount(BigDecimal(500)),
                    earnType = EarnType.SYSTEM,
                    earnedAt = LocalDateTime.now(),
                    period = expirationPeriod(30),
                )
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(1_000)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = laterExpiring, walletId = wallet.id, policyId = "policy-1")
            earningRepository.save(earning = soonExpiring, walletId = wallet.id, policyId = "policy-1")
            val usePointService: UsePointService = service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("포인트를 사용하면") {
                usePointService.use(
                    UsePointDto(
                        memberId = "member-1",
                        orderNumber = "ORDER-2",
                        amount = BigDecimal(500),
                    ),
                )

                Then("만료 임박한 적립건이 먼저 소진된다") {
                    soonExpiring.remainingAmount.value shouldBe BigDecimal.ZERO
                    laterExpiring.remainingAmount.value shouldBe BigDecimal(500)
                }
            }
        }

        Given("포인트 사용을 요청하면") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(1_000)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
            val eventPublisher: FakeUseEventPublisher = FakeUseEventPublisher()
            val usePointService: UsePointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)

            When("사용을 실행하면") {
                val usage: PointUsage =
                    usePointService
                        .use(UsePointDto(memberId = "member-1", orderNumber = "ORDER-USE", amount = BigDecimal(300)))
                        .pointUsage

                Then("PointsUsed 이벤트가 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsUsed = eventPublisher.publishedEvents[0] as PointsUsed
                    event.walletId shouldBe wallet.id
                    event.amount shouldBe BigDecimal(-300)
                    event.balanceAfter shouldBe BigDecimal(700)
                    event.usageId shouldBe usage.id
                }
            }
        }

        Given("가용 포인트보다 많은 금액을 사용하려고 하면") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(500)))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(500)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
            val usePointService: UsePointService = service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("사용을 시도하면") {
                Then("예외가 발생하고 잔액은 변하지 않는다") {
                    shouldThrow<PointDomainException> {
                        usePointService.use(
                            UsePointDto(
                                memberId = "member-1",
                                orderNumber = "ORDER-3",
                                amount = BigDecimal(600),
                            ),
                        )
                    }
                    wallet.balance.amount shouldBe BigDecimal(500)
                }
            }
        }

        Given("만료되지 않은 적립건에서 차감된 사용 건을 전액 취소하면") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(300))
            val usage: PointUsage = pointUsage(lines = listOf(UsageLine(earningId = "e1", amount = BigDecimal(300))))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(700)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
            val usageRepository: FakePointUsageRepository = FakePointUsageRepository()
            usageRepository.save(usage = usage, walletId = wallet.id)
            val eventPublisher: FakeUseEventPublisher = FakeUseEventPublisher()
            val usePointService: UsePointService =
                service(
                    walletRepository = walletRepository,
                    earningRepository = earningRepository,
                    usageRepository = usageRepository,
                    eventPublisher = eventPublisher,
                )

            When("사용취소를 실행하면") {
                val result: CancelUsagePointResultDto =
                    usePointService.cancelUsage(
                        CancelUsagePointDto(memberId = "member-1", usageId = usage.id, requestId = "cancel-req-1"),
                    )

                Then("원 적립건이 그대로 복원되고 신규 적립은 생기지 않는다") {
                    result.reEarnings.shouldBeEmpty()
                    result.requestedLines.size shouldBe 1
                    result.requestedLines[0].restorationType shouldBe RestorationType.RESTORED
                    earning.remainingAmount.value shouldBe BigDecimal(1_000)
                    wallet.balance.amount shouldBe BigDecimal(1_000)
                    result.usage.status shouldBe UsageStatus.FULLY_CANCELED
                }

                Then("PointsUsageCancelled 이벤트가 saveCancellation에 전달된 것과 같은 cancellationId로 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsUsageCancelled = eventPublisher.publishedEvents[0] as PointsUsageCancelled
                    event.walletId shouldBe wallet.id
                    event.amount shouldBe BigDecimal(300)
                    event.balanceAfter shouldBe BigDecimal(1_000)
                    event.cancellationId shouldBe usageRepository.lastSaveCancellationCall?.cancellationId
                }
            }
        }

        Given("이미 만료된 적립건에서 차감된 사용 건을 취소하면") {
            val expiredEarning: PointEarning =
                pointEarning(
                    id = "expired-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            expiredEarning.use(BigDecimal(400))
            val usage: PointUsage = pointUsage(lines = listOf(UsageLine(earningId = "expired-1", amount = BigDecimal(400))))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(600)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = expiredEarning, walletId = wallet.id, policyId = "policy-1")
            val usageRepository: FakePointUsageRepository = FakePointUsageRepository()
            usageRepository.save(usage = usage, walletId = wallet.id)
            val usePointService: UsePointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, usageRepository = usageRepository)

            When("사용취소를 실행하면") {
                val result: CancelUsagePointResultDto =
                    usePointService.cancelUsage(
                        CancelUsagePointDto(memberId = "member-1", usageId = usage.id, requestId = "cancel-req-2"),
                    )

                Then("만료된 적립건은 복원되지 않고 신규 적립으로 대체된다") {
                    result.reEarnings.size shouldBe 1
                    result.reEarnings[0].amount.value shouldBe BigDecimal(400)
                    result.requestedLines[0].restorationType shouldBe RestorationType.RE_EARNED
                    expiredEarning.remainingAmount.value shouldBe BigDecimal(600)
                    wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("사용 건의 일부만 취소하면") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(500))
            val usage: PointUsage = pointUsage(lines = listOf(UsageLine(earningId = "e1", amount = BigDecimal(500))))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(500)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
            val usageRepository: FakePointUsageRepository = FakePointUsageRepository()
            usageRepository.save(usage = usage, walletId = wallet.id)
            val usePointService: UsePointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, usageRepository = usageRepository)

            When("일부 금액만 취소하면") {
                val result: CancelUsagePointResultDto =
                    usePointService.cancelUsage(
                        CancelUsagePointDto(
                            memberId = "member-1",
                            usageId = usage.id,
                            requestId = "cancel-req-3",
                            amount = BigDecimal(200),
                        ),
                    )

                Then("사용 건은 부분취소 상태가 되고 남은 사용 금액이 갱신된다") {
                    result.usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                    result.usage.remainingAmount shouldBe BigDecimal(300)
                    wallet.balance.amount shouldBe BigDecimal(700)
                }
            }
        }

        Given("회원이 주문에서 포인트를 사용한 이력이 있을 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(1_000)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
            val usePointService: UsePointService = service(walletRepository = walletRepository, earningRepository = earningRepository)
            val usage: PointUsage =
                usePointService
                    .use(
                        UsePointDto(memberId = "member-1", orderNumber = "ORDER-9", amount = BigDecimal(300)),
                    ).pointUsage

            When("사용건 목록을 조회하면") {
                val usages: List<PointUsageResultDto> = usePointService.getUsages("member-1")

                Then("사용건이 반환된다") {
                    usages.size shouldBe 1
                    usages[0].pointUsage.id shouldBe usage.id
                }
            }

            When("사용건 상세를 조회하면") {
                val found: PointUsage = usePointService.getUsage(usage.id).pointUsage

                Then("동일한 사용건이 반환된다") {
                    found.id shouldBe usage.id
                    found.orderNumber shouldBe usage.orderNumber
                }
            }
        }
    })
