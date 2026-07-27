package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RemainingAmountTest :
    BehaviorSpec({
        Given("0 이상의 값이 주어지면") {
            When("RemainingAmount를 생성하면") {
                val remaining: RemainingAmount = RemainingAmount(BigDecimal(500))

                Then("정상적으로 생성된다") {
                    remaining.value shouldBe BigDecimal(500)
                }
            }
        }

        Given("0이 주어지면") {
            When("RemainingAmount를 생성하면") {
                val remaining: RemainingAmount = RemainingAmount(BigDecimal.ZERO)

                Then("허용된다") {
                    remaining.value shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("음수가 주어지면") {
            When("RemainingAmount를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        RemainingAmount(BigDecimal(-1))
                    }
                }
            }
        }

        Given("잔여액이 0인 RemainingAmount가 주어지면") {
            When("isExhausted를 호출하면") {
                Then("true를 반환한다") {
                    RemainingAmount(BigDecimal.ZERO).isExhausted() shouldBe true
                }
            }
        }

        Given("잔여액이 0보다 큰 RemainingAmount가 주어지면") {
            When("isExhausted를 호출하면") {
                Then("false를 반환한다") {
                    RemainingAmount(BigDecimal.ONE).isExhausted() shouldBe false
                }
            }
        }

        Given("최초 적립액과 동일한 잔여액이 주어지면") {
            val amount: PointAmount = pointAmount(BigDecimal(1_000))

            When("isFullAmountOf를 호출하면") {
                Then("true를 반환한다") {
                    RemainingAmount(BigDecimal(1_000)).isFullAmountOf(amount) shouldBe true
                }
            }
        }

        Given("최초 적립액보다 작은 잔여액이 주어지면") {
            val amount: PointAmount = pointAmount(BigDecimal(1_000))

            When("isFullAmountOf를 호출하면") {
                Then("false를 반환한다") {
                    RemainingAmount(BigDecimal(999)).isFullAmountOf(amount) shouldBe false
                }
            }
        }

        Given("잔여액이 1,000원인 RemainingAmount가 주어지면") {
            val remaining: RemainingAmount = RemainingAmount(BigDecimal(1_000))

            When("1 이상 잔여액 이하의 금액을 차감하면") {
                val result: RemainingAmount = remaining.decrease(BigDecimal(300))

                Then("차감된 금액이 반영된다") {
                    result.value shouldBe BigDecimal(700)
                }
            }

            When("잔여액 전부를 차감하면") {
                val result: RemainingAmount = remaining.decrease(BigDecimal(1_000))

                Then("0이 된다") {
                    result.value.signum() shouldBe 0
                }
            }

            When("차감액이 0 이하이면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        remaining.decrease(BigDecimal.ZERO)
                    }
                }
            }

            When("차감액이 잔여액을 초과하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        remaining.decrease(BigDecimal(1_001))
                    }
                }
            }
        }

        Given("잔여액이 700원인 RemainingAmount와 최초 적립액 1,000원이 주어지면") {
            val upTo: PointAmount = pointAmount(BigDecimal(1_000))
            val remaining: RemainingAmount = RemainingAmount(BigDecimal(700))

            When("최초 적립액 이하로 복원하면") {
                val result: RemainingAmount = remaining.increase(amount = BigDecimal(300), upTo = upTo)

                Then("복원된 금액이 반영된다") {
                    result.value shouldBe BigDecimal(1_000)
                }
            }

            When("복원액이 0 이하이면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        remaining.increase(amount = BigDecimal.ZERO, upTo = upTo)
                    }
                }
            }

            When("복원 후 금액이 최초 적립액을 초과하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        remaining.increase(amount = BigDecimal(301), upTo = upTo)
                    }
                }
            }
        }
    })
