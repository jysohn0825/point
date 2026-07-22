package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HoldingLimitTest :
    FunSpec({
        test("value가 0보다 크면 HoldingLimit을 생성한다") {
            val limit = holdingLimit(value = 1_000L)

            limit.value shouldBe 1_000L
        }

        test("value가 0 이하이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { HoldingLimit(0L) }
        }

        test("잔액과 적립금액의 합이 한도 이하이면 수용 가능하다") {
            val limit = holdingLimit(value = 100L)

            limit.canAccept(balance(amount = 60L), pointAmount(value = 40L)) shouldBe true
        }

        test("잔액과 적립금액의 합이 한도를 초과하면 수용할 수 없다") {
            val limit = holdingLimit(value = 100L)

            limit.canAccept(balance(amount = 60L), pointAmount(value = 41L)) shouldBe false
        }
    })
