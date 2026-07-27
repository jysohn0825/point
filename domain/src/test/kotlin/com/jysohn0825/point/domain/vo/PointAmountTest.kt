package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointAmountTest :
    BehaviorSpec({
        Given("양수 금액이 주어지면") {
            When("PointAmount를 생성하면") {
                val amount: PointAmount = PointAmount(BigDecimal(1_000))

                Then("정상적으로 생성된다") {
                    amount.value shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("최소값인 1원이 주어지면") {
            When("PointAmount를 생성하면") {
                val amount: PointAmount = PointAmount(BigDecimal.ONE)

                Then("정상적으로 생성된다") {
                    amount.value shouldBe BigDecimal.ONE
                }
            }
        }

        Given("0원이 주어지면") {
            When("PointAmount를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        PointAmount(BigDecimal.ZERO)
                    }
                }
            }
        }

        Given("음수가 주어지면") {
            When("PointAmount를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        PointAmount(BigDecimal(-1))
                    }
                }
            }
        }
    })
