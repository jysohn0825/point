package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class HoldingLimitTest :
    FunSpec({
        test("value가 0보다 크면 HoldingLimit을 생성한다") {
            val limit: HoldingLimit = holdingLimit(value = BigDecimal(1_000))

            limit.value shouldBe BigDecimal(1_000)
        }

        test("value가 0 이하이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { HoldingLimit(BigDecimal.ZERO) }
        }

        test("잔액과 적립금액의 합이 한도 이하이면 수용 가능하다") {
            val limit: HoldingLimit = holdingLimit(value = BigDecimal(100))

            limit.canAccept(
                balance = balance(amount = BigDecimal(60)),
                pointAmount = pointAmount(value = BigDecimal(40)),
            ) shouldBe true
        }

        test("잔액과 적립금액의 합이 한도를 초과하면 수용할 수 없다") {
            val limit: HoldingLimit = holdingLimit(value = BigDecimal(100))

            limit.canAccept(
                balance = balance(amount = BigDecimal(60)),
                pointAmount = pointAmount(value = BigDecimal(41)),
            ) shouldBe false
        }
    })
