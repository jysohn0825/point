package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.fixture.earningUsageTrace
import com.jysohn0825.point.domain.fixture.orderNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class EarningUsageTraceTest :
    FunSpec({
        test("차감액이 0보다 크면 생성할 수 있다") {
            val number: OrderNumber = orderNumber()

            val trace: EarningUsageTrace = EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000))

            trace.orderNumber shouldBe number
            trace.amount shouldBe BigDecimal(1_000)
        }

        test("차감액이 0이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                EarningUsageTrace(orderNumber = orderNumber(), amount = BigDecimal.ZERO)
            }
        }

        test("차감액이 음수이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                EarningUsageTrace(orderNumber = orderNumber(), amount = BigDecimal(-1))
            }
        }

        test("같은 값을 가진 EarningUsageTrace는 서로 동등하다") {
            val number: OrderNumber = orderNumber()

            EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000)) shouldBe
                EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000))
        }

        test("다른 금액을 가진 EarningUsageTrace는 서로 동등하지 않다") {
            val number: OrderNumber = orderNumber()

            (
                EarningUsageTrace(orderNumber = number, amount = BigDecimal(1_000)) ==
                    EarningUsageTrace(orderNumber = number, amount = BigDecimal(2_000))
            ) shouldBe false
        }

        test("copy로 금액만 변경한 새 인스턴스를 만들 수 있다") {
            val trace: EarningUsageTrace = earningUsageTrace(amount = BigDecimal(1_000))

            val copied: EarningUsageTrace = trace.copy(amount = BigDecimal(500))

            copied.orderNumber shouldBe trace.orderNumber
            copied.amount shouldBe BigDecimal(500)
        }
    })
