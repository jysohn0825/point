package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PointAmountTest : FunSpec({
    test("value가 0보다 크면 PointAmount를 생성한다") {
        val amount = pointAmount(value = 500L)

        amount.value shouldBe 500L
    }

    test("value가 0 이하이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> { PointAmount(0L) }
        shouldThrow<IllegalArgumentException> { PointAmount(-1L) }
    }
})
