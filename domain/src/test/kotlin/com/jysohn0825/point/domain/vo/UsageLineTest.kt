package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class UsageLineTest :
    BehaviorSpec({
        Given("차감액이 0보다 크면") {
            val earningId: String = "earning-1"

            When("UsageLine을 생성하면") {
                val line: UsageLine = UsageLine(earningId = earningId, amount = BigDecimal(1_000))

                Then("정상적으로 생성된다") {
                    line.earningId shouldBe earningId
                    line.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("차감액이 0이면") {
            When("UsageLine을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { UsageLine(earningId = "earning-1", amount = BigDecimal.ZERO) }
                }
            }
        }

        Given("차감액이 음수이면") {
            When("UsageLine을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { UsageLine(earningId = "earning-1", amount = BigDecimal(-1)) }
                }
            }
        }

        Given("같은 값을 가진 두 UsageLine이 주어지면") {
            val earningId: String = "earning-1"

            When("동등성을 비교하면") {
                Then("서로 동등하다") {
                    UsageLine(earningId = earningId, amount = BigDecimal(1_000)) shouldBe
                        UsageLine(earningId = earningId, amount = BigDecimal(1_000))
                }
            }
        }

        Given("금액만 다른 두 UsageLine이 주어지면") {
            val earningId: String = "earning-1"

            When("동등성을 비교하면") {
                Then("서로 동등하지 않다") {
                    (
                        UsageLine(earningId = earningId, amount = BigDecimal(1_000)) ==
                            UsageLine(earningId = earningId, amount = BigDecimal(2_000))
                    ) shouldBe false
                }
            }
        }

        Given("UsageLine이 주어지면") {
            val line: UsageLine = usageLine(amount = BigDecimal(1_000))

            When("copy로 금액만 변경하면") {
                val copied: UsageLine = line.copy(amount = BigDecimal(500))

                Then("나머지 값은 유지된 새 인스턴스가 만들어진다") {
                    copied.earningId shouldBe line.earningId
                    copied.amount shouldBe BigDecimal(500)
                }
            }
        }
    })
