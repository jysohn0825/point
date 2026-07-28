package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.EarnPointDto
import com.jysohn0825.point.application.service.dto.EarningUsageTraceResultDto
import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.event.PointsEarned
import com.jysohn0825.point.domain.event.PointsEarningCancelled
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointPolicyRepository
import com.jysohn0825.point.domain.repository.FakePointUsageRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicLong

private class FakeEarnKeyGenerator : DistributedKeyGenerator {
    private val counter: AtomicLong = AtomicLong()

    override fun next(name: String): Long = counter.incrementAndGet()
}

private class FakeEarnEventPublisher : ApplicationEventPublisher {
    val publishedEvents: MutableList<Any> = mutableListOf()

    override fun publishEvent(event: Any) {
        publishedEvents.add(event)
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository = FakePointEarningRepository(),
    policyRepository: FakePointPolicyRepository = FakePointPolicyRepository(),
    usageRepository: FakePointUsageRepository = FakePointUsageRepository(),
    keyGenerator: DistributedKeyGenerator = FakeEarnKeyGenerator(),
    eventPublisher: ApplicationEventPublisher = FakeEarnEventPublisher(),
): EarnPointService =
    EarnPointService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        policyRepository = policyRepository,
        usageRepository = usageRepository,
        keyGenerator = keyGenerator,
        eventPublisher = eventPublisher,
    )

class EarnPointServiceTest :
    BehaviorSpec({
        Given("처음 보는 sourceReferenceId로 적립을 요청하면") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val eventPublisher: FakeEarnEventPublisher = FakeEarnEventPublisher()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)

            When("적립을 실행하면") {
                val result: PointEarningResultDto =
                    earnPointService.systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-1",
                        ),
                    )
                val earning: PointEarning = result.pointEarning

                Then("새 적립건이 생성되고 지갑 잔액이 증가한다") {
                    earning.amount.value shouldBe BigDecimal(1_000)
                    earning.sourceReferenceId shouldBe "ORDER-1"
                    earningRepository.findAllByWalletId(walletId = wallet.id).size shouldBe 1
                    wallet.balance.amount shouldBe BigDecimal(1_000)
                }

                Then("PointsEarned 이벤트가 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsEarned = eventPublisher.publishedEvents[0] as PointsEarned
                    event.walletId shouldBe wallet.id
                    event.amount shouldBe BigDecimal(1_000)
                    event.balanceAfter shouldBe BigDecimal(1_000)
                    event.earningId shouldBe earning.id
                }
            }
        }

        Given("이미 처리된 sourceReferenceId로 동일한 적립을 다시 요청하면") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val eventPublisher: FakeEarnEventPublisher = FakeEarnEventPublisher()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)
            val dto: EarnPointDto =
                EarnPointDto(
                    memberId = "member-1",
                    amount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM,
                    sourceReferenceId = "ORDER-1",
                )

            val firstEarning: PointEarning = earnPointService.systemEarn(dto).pointEarning

            /**
             * 서비스는 더 이상 findExistingEarning으로 기존 적립건을 조회해 그대로 반환하지 않는다.
             * 재시도 요청은 항상 새 적립건 insert를 시도하고, 같은 (walletId, earnType, sourceReferenceId)
             * 조합은 DB 유니크 제약(uk_earning_source) 위반으로 커밋에 실패해 409로 응답한다
             * (GlobalExceptionHandler 참고). 이 fake repository는 DB 제약을 흉내내지 않으므로,
             * 여기서는 "앱 레이어에서 더 이상 dedup하지 않는다"는 사실만 검증한다.
             */
            When("같은 요청을 재시도하면") {
                val secondEarning: PointEarning = earnPointService.systemEarn(dto).pointEarning

                Then("기존 적립건을 재사용하지 않고 새 적립건을 만든다") {
                    secondEarning.id shouldNotBe firstEarning.id
                    earningRepository.findAllByWalletId(walletId = wallet.id).size shouldBe 2
                    wallet.balance.amount shouldBe BigDecimal(2_000)
                }

                Then("새 적립이 발생했으므로 이벤트가 추가로 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 2
                }
            }
        }

        Given("1회 적립 한도를 초과하는 금액으로 요청하면") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val policyRepository: FakePointPolicyRepository = FakePointPolicyRepository()
            policyRepository.reset(policy = pointPolicy(maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(10_000))))
            val earnPointService: EarnPointService = service(walletRepository = walletRepository, policyRepository = policyRepository)

            When("적립을 시도하면") {
                Then("예외가 발생하고 잔액은 변하지 않는다") {
                    shouldThrow<PointDomainException> {
                        earnPointService.systemEarn(
                            EarnPointDto(
                                memberId = "member-1",
                                amount = BigDecimal(10_001),
                                earnType = EarnType.SYSTEM,
                                sourceReferenceId = "ORDER-2",
                            ),
                        )
                    }
                    wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("사용되지 않은 적립건을 취소하면") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val eventPublisher: FakeEarnEventPublisher = FakeEarnEventPublisher()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)
            val earning: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-3",
                        ),
                    ).pointEarning

            When("적립 취소를 실행하면") {
                eventPublisher.publishedEvents.clear()
                val canceled: PointEarning = earnPointService.cancelEarning(memberId = "member-1", earningId = earning.id).pointEarning

                Then("적립건이 취소 상태가 되고 지갑 잔액이 원복된다") {
                    canceled.status shouldBe EarningStatus.CANCELED
                    canceled.remainingAmount.value shouldBe BigDecimal.ZERO
                    wallet.balance.amount shouldBe BigDecimal.ZERO
                }

                Then("PointsEarningCancelled 이벤트가 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsEarningCancelled = eventPublisher.publishedEvents[0] as PointsEarningCancelled
                    event.walletId shouldBe wallet.id
                    event.amount shouldBe BigDecimal(-1_000)
                    event.balanceAfter shouldBe BigDecimal.ZERO
                    event.earningId shouldBe earning.id
                }
            }
        }

        Given("일부 사용된 적립건을 취소하려고 하면") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val earnPointService: EarnPointService = service(walletRepository = walletRepository, earningRepository = earningRepository)
            val earning: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-4",
                        ),
                    ).pointEarning
            earning.use(BigDecimal(400))

            When("적립 취소를 시도하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earnPointService.cancelEarning(memberId = "member-1", earningId = earning.id)
                    }
                }
            }
        }

        Given("잔여액이 남은 적립건이 있을 때") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val eventPublisher: FakeEarnEventPublisher = FakeEarnEventPublisher()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)
            val earning: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-8",
                        ),
                    ).pointEarning

            When("관리자가 강제 즉시 만료를 실행하면") {
                eventPublisher.publishedEvents.clear()
                val expired: PointEarning =
                    earnPointService.forceExpireEarning(memberId = "member-1", earningId = earning.id).pointEarning

                Then("잔여액이 소멸되고 지갑 잔액이 차감된다") {
                    expired.status shouldBe EarningStatus.EXPIRED
                    expired.remainingAmount.value shouldBe BigDecimal.ZERO
                    wallet.balance.amount shouldBe BigDecimal.ZERO
                }

                Then("PointsExpired 이벤트가 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsExpired = eventPublisher.publishedEvents[0] as PointsExpired
                    event.walletId shouldBe wallet.id
                    event.amount shouldBe BigDecimal(-1_000)
                    event.balanceAfter shouldBe BigDecimal.ZERO
                    event.earningId shouldBe earning.id
                }
            }
        }

        Given("이미 전액 사용된 적립건이 있을 때") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val eventPublisher: FakeEarnEventPublisher = FakeEarnEventPublisher()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)
            val earning: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-9",
                        ),
                    ).pointEarning
            earning.use(BigDecimal(1_000))

            When("관리자가 강제 즉시 만료를 실행하면") {
                eventPublisher.publishedEvents.clear()
                val result: PointEarning = earnPointService.forceExpireEarning(memberId = "member-1", earningId = earning.id).pointEarning

                Then("소멸할 잔여액이 없어 상태는 EXHAUSTED로 유지되고 지갑 잔액도 변하지 않는다") {
                    result.status shouldBe EarningStatus.EXHAUSTED
                    wallet.balance.amount shouldBe BigDecimal(1_000)
                }

                Then("지갑 변동이 없으므로 이벤트가 발행되지 않는다") {
                    eventPublisher.publishedEvents.size shouldBe 0
                }
            }
        }

        Given("적립건이 여러 개 있을 때") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val earnPointService: EarnPointService = service(walletRepository = walletRepository, earningRepository = earningRepository)
            earnPointService.systemEarn(
                EarnPointDto(
                    memberId = "member-1",
                    amount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM,
                    sourceReferenceId = "ORDER-5",
                ),
            )
            val second: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(500),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-6",
                        ),
                    ).pointEarning

            When("목록을 조회하면") {
                val earnings: List<PointEarningResultDto> = earnPointService.getEarnings("member-1")

                Then("회원의 전체 적립건이 반환된다") {
                    earnings.size shouldBe 2
                }
            }

            When("상세를 조회하면") {
                val earning: PointEarning = earnPointService.getEarning(second.id).pointEarning

                Then("해당 적립건이 반환된다") {
                    earning.id shouldBe second.id
                    earning.amount.value shouldBe BigDecimal(500)
                }
            }
        }

        Given("적립건이 특정 주문에서 사용된 이력이 있을 때") {
            val wallet: PointWallet = pointWallet()
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            val usageRepository: FakePointUsageRepository = FakePointUsageRepository()
            val earnPointService: EarnPointService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, usageRepository = usageRepository)
            val earning: PointEarning =
                earnPointService
                    .systemEarn(
                        EarnPointDto(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-7",
                        ),
                    ).pointEarning
            earning.use(BigDecimal(300))
            usageRepository.save(
                usage =
                    PointUsage.use(
                        id = "usage-1",
                        orderNumber = OrderNumber("A1234"),
                        lines = listOf(UsageLine(earningId = earning.id, amount = BigDecimal(300))),
                    ),
                walletId = wallet.id,
            )

            When("사용 추적을 조회하면") {
                val traces: List<EarningUsageTraceResultDto> = earnPointService.getUsageTraces(earning.id)

                Then("차감된 주문번호와 금액이 반환된다") {
                    traces.size shouldBe 1
                    traces[0].earningUsageTrace.orderNumber.value shouldBe "A1234"
                    traces[0].earningUsageTrace.amount shouldBe BigDecimal(300)
                }
            }
        }
    })
