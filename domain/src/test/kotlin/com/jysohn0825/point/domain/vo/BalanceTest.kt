package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class BalanceTest :
    BehaviorSpec({
        Given("0 이상의 금액이 주어지면") {
            When("Balance를 생성하면") {
                val result: Balance = balance(amount = BigDecimal(100))

                Then("해당 금액으로 생성된다") {
                    result.amount shouldBe BigDecimal(100)
                }
            }
        }

        Given("음수 금액이 주어지면") {
            When("Balance를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { Balance(BigDecimal(-1)) }
                }
            }
        }

        Given("ZERO 상수가 주어지면") {
            When("금액을 조회하면") {
                Then("0원이다") {
                    Balance.ZERO.amount shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("Balance가 주어지면") {
            val origin: Balance = balance(amount = BigDecimal(100))

            When("PointAmount만큼 더하면") {
                val result: Balance = origin + pointAmount(value = BigDecimal(50))

                Then("증가한 새 Balance를 반환하고 원본은 변경되지 않는다") {
                    result.amount shouldBe BigDecimal(150)
                    origin.amount shouldBe BigDecimal(100)
                }
            }

            When("PointAmount만큼 빼면") {
                val result: Balance = origin - pointAmount(value = BigDecimal(30))

                Then("감소한 새 Balance를 반환한다") {
                    result.amount shouldBe BigDecimal(70)
                }
            }
        }

        Given("차감 결과가 음수가 되는 상황이 주어지면") {
            val origin: Balance = balance(amount = BigDecimal(10))

            When("PointAmount를 빼면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { origin - pointAmount(value = BigDecimal(20)) }
                }
            }
        }
    })
