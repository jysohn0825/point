package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RemainingAmountTest : FunSpec({
    test("0 이상의 값이면 정상적으로 생성된다") {
        val remaining = RemainingAmount(500L)

        remaining.value shouldBe 500L
    }

    test("0은 허용한다") {
        val remaining = RemainingAmount(0L)

        remaining.value shouldBe 0L
    }

    test("음수이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            RemainingAmount(-1L)
        }
    }

    context("isExhausted") {
        test("잔여액이 0이면 true를 반환한다") {
            RemainingAmount(0L).isExhausted() shouldBe true
        }

        test("잔여액이 0보다 크면 false를 반환한다") {
            RemainingAmount(1L).isExhausted() shouldBe false
        }
    }

    context("isFullAmountOf") {
        test("최초 적립액과 동일하면 true를 반환한다") {
            val amount = PointAmount(1_000L)

            RemainingAmount(1_000L).isFullAmountOf(amount) shouldBe true
        }

        test("최초 적립액보다 작으면 false를 반환한다") {
            val amount = PointAmount(1_000L)

            RemainingAmount(999L).isFullAmountOf(amount) shouldBe false
        }
    }

    context("decrease") {
        test("1 이상 잔여액 이하의 금액을 차감할 수 있다") {
            val remaining = RemainingAmount(1_000L)

            val result = remaining.decrease(300L)

            result.value shouldBe 700L
        }

        test("잔여액 전부를 차감하면 0이 된다") {
            val remaining = RemainingAmount(1_000L)

            val result = remaining.decrease(1_000L)

            result.value shouldBe 0L
        }

        test("차감액이 0 이하이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                RemainingAmount(1_000L).decrease(0L)
            }
        }

        test("차감액이 잔여액을 초과하면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                RemainingAmount(1_000L).decrease(1_001L)
            }
        }
    }

    context("increase") {
        test("최초 적립액 이하로 복원할 수 있다") {
            val upTo = PointAmount(1_000L)
            val remaining = RemainingAmount(700L)

            val result = remaining.increase(300L, upTo)

            result.value shouldBe 1_000L
        }

        test("복원액이 0 이하이면 예외가 발생한다") {
            val upTo = PointAmount(1_000L)

            shouldThrow<IllegalArgumentException> {
                RemainingAmount(700L).increase(0L, upTo)
            }
        }

        test("복원 후 금액이 최초 적립액을 초과하면 예외가 발생한다") {
            val upTo = PointAmount(1_000L)

            shouldThrow<IllegalArgumentException> {
                RemainingAmount(700L).increase(301L, upTo)
            }
        }
    }
})
