package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MaxHoldingAmountTest :
    FunSpec({

        test("하한값(1포인트)으로 생성할 수 있다") {
            val sut = MaxHoldingAmount(BigDecimal.ONE)

            sut.value shouldBe BigDecimal.ONE
        }

        test("1포인트 미만이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                MaxHoldingAmount(BigDecimal.ZERO)
            }
        }

        test("픽스처는 유효한 기본값으로 생성된다") {
            val sut = maxHoldingAmount()

            sut.value shouldBe BigDecimal(1_000_000)
        }
    })
