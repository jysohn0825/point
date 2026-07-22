package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.fixture.earningId
import com.jysohn0825.point.domain.fixture.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UsageLineTest : FunSpec({
    test("차감액이 0보다 크면 생성할 수 있다") {
        val earningId = earningId()

        val line = UsageLine(earningId, 1_000L)

        line.earningId shouldBe earningId
        line.amount shouldBe 1_000L
    }

    test("차감액이 0이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> { UsageLine(earningId(), 0L) }
    }

    test("차감액이 음수이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> { UsageLine(earningId(), -1L) }
    }

    test("같은 값을 가진 UsageLine은 서로 동등하다") {
        val earningId = earningId()

        UsageLine(earningId, 1_000L) shouldBe UsageLine(earningId, 1_000L)
    }

    test("다른 금액을 가진 UsageLine은 서로 동등하지 않다") {
        val earningId = earningId()

        (UsageLine(earningId, 1_000L) == UsageLine(earningId, 2_000L)) shouldBe false
    }

    test("copy로 금액만 변경한 새 인스턴스를 만들 수 있다") {
        val line = usageLine(amount = 1_000L)

        val copied = line.copy(amount = 500L)

        copied.earningId shouldBe line.earningId
        copied.amount shouldBe 500L
    }

    test("toString은 필드 값을 포함한다") {
        val line = usageLine(amount = 1_000L)

        line.toString() shouldBe "UsageLine(earningId=${line.earningId}, amount=1000)"
    }
})
