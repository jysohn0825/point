package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.earningUsageTrace
import com.jysohn0825.point.domain.vo.orderNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class EarningUsageTraceTest :
    BehaviorSpec({
        Given("차감액이 0보다 크면") {
            val number: OrderNumber = orderNumber()

            When("EarningUsageTrace를 생성하면") {
                val trace: EarningUsageTrace = EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000))

                Then("정상적으로 생성된다") {
                    trace.orderNumber shouldBe number
                    trace.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("차감액이 0이면") {
            When("EarningUsageTrace를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        EarningUsageTrace(orderNumber = orderNumber(), amount = BigDecimal.ZERO)
                    }
                }
            }
        }

        Given("차감액이 음수이면") {
            When("EarningUsageTrace를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        EarningUsageTrace(orderNumber = orderNumber(), amount = BigDecimal(-1))
                    }
                }
            }
        }

        Given("같은 값을 가진 두 EarningUsageTrace가 주어지면") {
            val number: OrderNumber = orderNumber()

            When("동등성을 비교하면") {
                Then("서로 동등하다") {
                    EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000)) shouldBe
                        EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000))
                }
            }
        }

        Given("금액만 다른 두 EarningUsageTrace가 주어지면") {
            val number: OrderNumber = orderNumber()

            When("동등성을 비교하면") {
                Then("서로 동등하지 않다") {
                    (
                        EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000)) ==
                            EarningUsageTrace(orderNumber = number, amount = BigDecimal(2_000))
                    ) shouldBe false
                }
            }
        }

        Given("EarningUsageTrace가 주어지면") {
            val trace: EarningUsageTrace = earningUsageTrace(amount = BigDecimal(1_000))

            When("copy로 금액만 변경하면") {
                val copied: EarningUsageTrace = trace.copy(amount = BigDecimal(500))

                Then("나머지 값은 유지된 새 인스턴스가 만들어진다") {
                    copied.orderNumber shouldBe trace.orderNumber
                    copied.amount shouldBe BigDecimal(500)
                }
            }
        }
    })
