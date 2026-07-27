package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class HoldingLimitTest :
    BehaviorSpec({
        Given("0보다 큰 값이 주어지면") {
            When("HoldingLimit을 생성하면") {
                val limit: HoldingLimit = holdingLimit(value = BigDecimal(1_000))

                Then("정상적으로 생성된다") {
                    limit.value shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("0 이하의 값이 주어지면") {
            When("HoldingLimit을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { HoldingLimit(BigDecimal.ZERO) }
                }
            }
        }

        Given("한도가 100원인 HoldingLimit이 주어지면") {
            val limit: HoldingLimit = holdingLimit(value = BigDecimal(100))

            When("잔액과 적립금액의 합이 한도 이하이면") {
                Then("수용 가능하다") {
                    limit.canAccept(
                        balance = balance(amount = BigDecimal(60)),
                        pointAmount = pointAmount(value = BigDecimal(40)),
                    ) shouldBe true
                }
            }

            When("잔액과 적립금액의 합이 한도를 초과하면") {
                Then("수용할 수 없다") {
                    limit.canAccept(
                        balance = balance(amount = BigDecimal(60)),
                        pointAmount = pointAmount(value = BigDecimal(41)),
                    ) shouldBe false
                }
            }
        }
    })
