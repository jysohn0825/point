package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.grantedBy
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class PointRedemptionAllocatorTest :
    BehaviorSpec({
        val sut: PointRedemptionAllocator = PointRedemptionAllocator()

        given("수기지급 적립건과 일반 적립건이 함께 있을 때") {
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

            `when`("일반 적립건보다 먼저 만료되지 않더라도 수기지급건을 사용하면") {
                then("수기지급 적립건이 먼저 차감된다") {
                    val lines: List<UsageLine> =
                        sut.allocate(earnings = listOf(systemEarning, manualEarning), amount = BigDecimal(500))

                    lines.size shouldBe 1
                    lines[0].earningId shouldBe "manual-1"
                    manualEarning.remainingAmount.value shouldBe BigDecimal(500)
                    systemEarning.remainingAmount.value shouldBe BigDecimal(1_000)
                }
            }
        }

        given("일반 적립건 중 만료일이 다른 두 건이 있을 때") {
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

            `when`("포인트를 사용하면") {
                then("만료 임박한 적립건이 먼저 소진된다") {
                    sut.allocate(earnings = listOf(laterExpiring, soonExpiring), amount = BigDecimal(500))

                    soonExpiring.remainingAmount.value shouldBe BigDecimal.ZERO
                    laterExpiring.remainingAmount.value shouldBe BigDecimal(500)
                }
            }
        }

        given("적립건 하나로는 요청 금액을 다 채울 수 없을 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(500)))

            `when`("가용 포인트보다 많은 금액을 배분하려 하면") {
                then("예외가 발생하고 적립건은 소진된 채로 남는다") {
                    shouldThrow<PointDomainException> {
                        sut.allocate(earnings = listOf(earning), amount = BigDecimal(600))
                    }
                    earning.remainingAmount.value shouldBe BigDecimal.ZERO
                }
            }
        }

        given("가용 적립건이 여러 개이고 요청 금액이 첫 적립건을 초과할 때") {
            val first: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(300)), earnedAt = LocalDateTime.now())
            val second: PointEarning = pointEarning(id = "e2", amount = pointAmount(BigDecimal(500)), earnedAt = LocalDateTime.now())

            `when`("두 적립건에 걸쳐 배분되면") {
                then("각 적립건에서 필요한 만큼만 차감된 사용 라인이 생성된다") {
                    val lines: List<UsageLine> = sut.allocate(earnings = listOf(first, second), amount = BigDecimal(600))

                    lines.size shouldBe 2
                    lines[0].earningId shouldBe "e1"
                    lines[0].amount shouldBe BigDecimal(300)
                    lines[1].earningId shouldBe "e2"
                    lines[1].amount shouldBe BigDecimal(300)
                    first.remainingAmount.value shouldBe BigDecimal.ZERO
                    second.remainingAmount.value shouldBe BigDecimal(200)
                }
            }
        }
    })
