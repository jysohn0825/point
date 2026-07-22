package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PointAmountTest :
    FunSpec({
        test("양수 금액이면 정상적으로 생성된다") {
            val amount = PointAmount(1_000L)

            amount.value shouldBe 1_000L
        }

        test("최소값인 1원도 허용한다") {
            val amount = PointAmount(1L)

            amount.value shouldBe 1L
        }

        test("0원이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                PointAmount(0L)
            }
        }

        test("음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                PointAmount(-1L)
            }
        }
    })
