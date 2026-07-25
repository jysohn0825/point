package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.fixture.usageLine
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class PointCancellationAllocatorTest :
    BehaviorSpec({
        val sut: PointCancellationAllocator = PointCancellationAllocator()
        val policy: PointPolicy = pointPolicy()
        val now: LocalDateTime = LocalDateTime.now()

        given("만료되지 않은 적립건에서 차감된 사용 건을 전액 취소하면") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(300))
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "e1", amount = BigDecimal(300))))

            `when`("취소를 배분하면") {
                then("원 적립건이 그대로 복원되고 신규 적립은 생기지 않는다") {
                    val allocation: CancellationAllocation =
                        sut.allocate(
                            usage = usage,
                            cancelAmount = BigDecimal(300),
                            earningsById = mapOf("e1" to earning),
                            policy = policy,
                            now = now,
                        )

                    allocation.reEarnings.shouldBeEmpty()
                    allocation.requestedLines.size shouldBe 1
                    allocation.requestedLines[0].restorationType shouldBe RestorationType.RESTORED
                    allocation.restoredEarnings shouldBe listOf(earning)
                    allocation.reearnedEarningIds shouldBe listOf(null)
                    earning.remainingAmount.value shouldBe BigDecimal(1_000)
                }
            }
        }

        given("이미 만료된 적립건에서 차감된 사용 건을 취소하면") {
            val expiredEarning: PointEarning =
                pointEarning(
                    id = "expired-1",
                    amount = pointAmount(BigDecimal(1_000)),
                    earnedAt = LocalDateTime.now().minusDays(10),
                    period = expirationPeriod(1),
                )
            expiredEarning.use(BigDecimal(400))
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "expired-1", amount = BigDecimal(400))))

            `when`("취소를 배분하면") {
                then("만료된 적립건은 복원되지 않고 신규 적립으로 대체된다") {
                    val allocation: CancellationAllocation =
                        sut.allocate(
                            usage = usage,
                            cancelAmount = BigDecimal(400),
                            earningsById = mapOf("expired-1" to expiredEarning),
                            policy = policy,
                            now = now,
                        )

                    allocation.reEarnings.size shouldBe 1
                    allocation.reEarnings[0].amount.value shouldBe BigDecimal(400)
                    allocation.requestedLines[0].restorationType shouldBe RestorationType.RE_EARNED
                    allocation.reearnedEarningIds shouldBe listOf(allocation.reEarnings[0].id)
                    allocation.restoredEarnings.shouldBeEmpty()
                    expiredEarning.remainingAmount.value shouldBe BigDecimal(600)
                }
            }
        }

        given("사용 건의 일부만 취소 요청할 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            earning.use(BigDecimal(500))
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "e1", amount = BigDecimal(500))))

            `when`("일부 금액만 배분하면") {
                then("요청한 금액만큼만 취소 라인이 생성된다") {
                    val allocation: CancellationAllocation =
                        sut.allocate(
                            usage = usage,
                            cancelAmount = BigDecimal(200),
                            earningsById = mapOf("e1" to earning),
                            policy = policy,
                            now = now,
                        )

                    allocation.requestedLines.size shouldBe 1
                    allocation.requestedLines[0].restoredAmount shouldBe BigDecimal(200)
                    earning.remainingAmount.value shouldBe BigDecimal(700)
                }
            }
        }

        given("취소할 사용 금액이 없을 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "e1", amount = BigDecimal(500))))

            `when`("0원을 취소 요청하면") {
                then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        sut.allocate(
                            usage = usage,
                            cancelAmount = BigDecimal.ZERO,
                            earningsById = mapOf("e1" to earning),
                            policy = policy,
                            now = now,
                        )
                    }
                }
            }
        }

        given("남은 사용 금액보다 많은 금액을 취소 요청할 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "e1", amount = BigDecimal(500))))

            `when`("남은 사용 금액을 초과해 취소 요청하면") {
                then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        sut.allocate(
                            usage = usage,
                            cancelAmount = BigDecimal(600),
                            earningsById = mapOf("e1" to earning),
                            policy = policy,
                            now = now,
                        )
                    }
                }
            }
        }
    })
