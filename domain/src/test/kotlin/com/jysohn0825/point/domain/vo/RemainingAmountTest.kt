package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RemainingAmountTest :
    FunSpec({
        test("0 이상의 값이면 정상적으로 생성된다") {
            val remaining: RemainingAmount = RemainingAmount(BigDecimal(500))

            remaining.value shouldBe BigDecimal(500)
        }

        test("0은 허용한다") {
            val remaining: RemainingAmount = RemainingAmount(BigDecimal.ZERO)

            remaining.value shouldBe BigDecimal.ZERO
        }

        test("음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                RemainingAmount(BigDecimal(-1))
            }
        }

        context("isExhausted") {
            test("잔여액이 0이면 true를 반환한다") {
                RemainingAmount(BigDecimal.ZERO).isExhausted() shouldBe true
            }

            test("잔여액이 0보다 크면 false를 반환한다") {
                RemainingAmount(BigDecimal.ONE).isExhausted() shouldBe false
            }
        }

        context("isFullAmountOf") {
            test("최초 적립액과 동일하면 true를 반환한다") {
                val amount: PointAmount = pointAmount(BigDecimal(1_000))

                RemainingAmount(BigDecimal(1_000)).isFullAmountOf(amount) shouldBe true
            }

            test("최초 적립액보다 작으면 false를 반환한다") {
                val amount: PointAmount = pointAmount(BigDecimal(1_000))

                RemainingAmount(BigDecimal(999)).isFullAmountOf(amount) shouldBe false
            }
        }

        context("decrease") {
            test("1 이상 잔여액 이하의 금액을 차감할 수 있다") {
                val remaining: RemainingAmount = RemainingAmount(BigDecimal(1_000))

                val result: RemainingAmount = remaining.decrease(BigDecimal(300))

                result.value shouldBe BigDecimal(700)
            }

            test("잔여액 전부를 차감하면 0이 된다") {
                val remaining: RemainingAmount = RemainingAmount(BigDecimal(1_000))

                val result: RemainingAmount = remaining.decrease(BigDecimal(1_000))

                result.value.signum() shouldBe 0
            }

            test("차감액이 0 이하이면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    RemainingAmount(BigDecimal(1_000)).decrease(BigDecimal.ZERO)
                }
            }

            test("차감액이 잔여액을 초과하면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    RemainingAmount(BigDecimal(1_000)).decrease(BigDecimal(1_001))
                }
            }
        }

        context("increase") {
            test("최초 적립액 이하로 복원할 수 있다") {
                val upTo: PointAmount = pointAmount(BigDecimal(1_000))
                val remaining: RemainingAmount = RemainingAmount(BigDecimal(700))

                val result: RemainingAmount = remaining.increase(amount = BigDecimal(300), upTo = upTo)

                result.value shouldBe BigDecimal(1_000)
            }

            test("복원액이 0 이하이면 예외가 발생한다") {
                val upTo: PointAmount = pointAmount(BigDecimal(1_000))

                shouldThrow<IllegalArgumentException> {
                    RemainingAmount(BigDecimal(700)).increase(amount = BigDecimal.ZERO, upTo = upTo)
                }
            }

            test("복원 후 금액이 최초 적립액을 초과하면 예외가 발생한다") {
                val upTo: PointAmount = pointAmount(BigDecimal(1_000))

                shouldThrow<IllegalArgumentException> {
                    RemainingAmount(BigDecimal(700)).increase(amount = BigDecimal(301), upTo = upTo)
                }
            }
        }
    })
