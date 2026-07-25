package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointAmountTest :
    FunSpec({
        test("양수 금액이면 정상적으로 생성된다") {
            val amount: PointAmount = PointAmount(BigDecimal(1_000))

            amount.value shouldBe BigDecimal(1_000)
        }

        test("최소값인 1원도 허용한다") {
            val amount: PointAmount = PointAmount(BigDecimal.ONE)

            amount.value shouldBe BigDecimal.ONE
        }

        test("0원이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                PointAmount(BigDecimal.ZERO)
            }
        }

        test("음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                PointAmount(BigDecimal(-1))
            }
        }
    })
