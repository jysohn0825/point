package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime

private class FakeExpirationEventPublisher : ApplicationEventPublisher {
    val publishedEvents: MutableList<Any> = mutableListOf()

    override fun publishEvent(event: Any) {
        publishedEvents.add(event)
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository,
    eventPublisher: ApplicationEventPublisher = FakeExpirationEventPublisher(),
): PointEarningExpirationService =
    PointEarningExpirationService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        eventPublisher = eventPublisher,
    )

class PointEarningExpirationServiceTest :
    BehaviorSpec({
        Given("이미 사용되어 EXHAUSTED 상태인 적립건만 있을 때") {
            val exhausted: PointEarning =
                pointEarning(
                    id = "exhausted-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            exhausted.use(BigDecimal(1_000))
            val wallet: PointWallet = pointWallet(balance = Balance.ZERO)
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = exhausted, walletId = wallet.id, policyId = "policy-1")
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("expireMemberEarningsNow를 호출하면") {
                val result: List<PointEarningResultDto> = expirationService.expireMemberEarningsNow("member-1")

                Then("잔여금액이 없으므로 만료 대상에서 제외되고 아무 것도 변하지 않는다") {
                    result.shouldBeEmpty()
                    exhausted.status shouldBe EarningStatus.EXHAUSTED
                    wallet.balance.amount shouldBe BigDecimal.ZERO
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
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(1_000)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = active, walletId = wallet.id, policyId = "policy-1")
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository)

            When("expireMemberEarningsNow를 호출하면") {
                val result: List<PointEarningResultDto> = expirationService.expireMemberEarningsNow("member-1")

                Then("아무 것도 처리하지 않는다") {
                    result.shouldBeEmpty()
                    active.status shouldBe EarningStatus.ACTIVE
                    wallet.balance.amount shouldBe BigDecimal(1_000)
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
            val wallet: PointWallet = pointWallet(balance = Balance(BigDecimal(700)))
            val walletRepository: FakePointWalletRepository = FakePointWalletRepository()
            walletRepository.seed(memberId = "member-1", wallet = wallet)
            val earningRepository: FakePointEarningRepository = FakePointEarningRepository()
            earningRepository.save(earning = expired, walletId = wallet.id, policyId = "policy-1")
            val eventPublisher: FakeExpirationEventPublisher = FakeExpirationEventPublisher()
            val expirationService: PointEarningExpirationService =
                service(walletRepository = walletRepository, earningRepository = earningRepository, eventPublisher = eventPublisher)

            When("expireMemberEarningsNow를 호출하면") {
                val result: List<PointEarningResultDto> = expirationService.expireMemberEarningsNow("member-1")

                Then("해당 회원의 만료 대상 적립건이 즉시 처리된다") {
                    result.size shouldBe 1
                    expired.status shouldBe EarningStatus.EXPIRED
                    wallet.balance.amount shouldBe BigDecimal.ZERO
                }

                Then("PointsExpired 이벤트가 발행된다") {
                    eventPublisher.publishedEvents.size shouldBe 1
                    val event: PointsExpired = eventPublisher.publishedEvents[0] as PointsExpired
                    event.earningId shouldBe expired.id
                    event.amount shouldBe BigDecimal(-700)
                }
            }
        }
    })
