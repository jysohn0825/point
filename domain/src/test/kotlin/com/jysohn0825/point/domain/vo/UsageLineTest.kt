package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.fixture.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class UsageLineTest :
    FunSpec({
        test("차감액이 0보다 크면 생성할 수 있다") {
            val earningId = "earning-1"

            val line = UsageLine(earningId, BigDecimal(1_000))

            line.earningId shouldBe earningId
            line.amount shouldBe BigDecimal(1_000)
        }

        test("차감액이 0이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { UsageLine("earning-1", BigDecimal.ZERO) }
        }

        test("차감액이 음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { UsageLine("earning-1", BigDecimal(-1)) }
        }

        test("같은 값을 가진 UsageLine은 서로 동등하다") {
            val earningId = "earning-1"

            UsageLine(earningId, BigDecimal(1_000)) shouldBe UsageLine(earningId, BigDecimal(1_000))
        }

        test("다른 금액을 가진 UsageLine은 서로 동등하지 않다") {
            val earningId = "earning-1"

            (UsageLine(earningId, BigDecimal(1_000)) == UsageLine(earningId, BigDecimal(2_000))) shouldBe false
        }

        test("copy로 금액만 변경한 새 인스턴스를 만들 수 있다") {
            val line = usageLine(amount = BigDecimal(1_000))

            val copied = line.copy(amount = BigDecimal(500))

            copied.earningId shouldBe line.earningId
            copied.amount shouldBe BigDecimal(500)
        }
    })
