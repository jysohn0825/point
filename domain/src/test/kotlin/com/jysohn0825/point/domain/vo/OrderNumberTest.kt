package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OrderNumberTest :
    FunSpec({
        test("공백이 아닌 값으로 생성할 수 있다") {
            val orderNumber: OrderNumber = OrderNumber("ORDER-20260722-0001")

            orderNumber.value shouldBe "ORDER-20260722-0001"
        }

        test("공백 문자열이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { OrderNumber("   ") }
        }

        test("빈 문자열이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { OrderNumber("") }
        }

        test("동일한 값을 가진 OrderNumber는 서로 동등하다") {
            (OrderNumber("ORDER-1") == OrderNumber("ORDER-1")) shouldBe true
        }

        test("다른 값을 가진 OrderNumber는 서로 동등하지 않다") {
            (OrderNumber("ORDER-1") == OrderNumber("ORDER-2")) shouldBe false
        }

        test("hashCode는 동일한 값에 대해 동일하다") {
            OrderNumber("ORDER-1").hashCode() shouldBe OrderNumber("ORDER-1").hashCode()
        }
    })
