package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MaxEarnPerTransactionTest : FunSpec({

    test("하한값(1포인트)으로 생성할 수 있다") {
        val sut = MaxEarnPerTransaction(1L)

        sut.value shouldBe 1L
    }

    test("상한값(10만포인트)으로 생성할 수 있다") {
        val sut = MaxEarnPerTransaction(100_000L)

        sut.value shouldBe 100_000L
    }

    test("1포인트 미만이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            MaxEarnPerTransaction(0L)
        }
    }

    test("10만포인트를 초과하면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            MaxEarnPerTransaction(100_001L)
        }
    }

    test("픽스처는 유효한 기본값으로 생성된다") {
        val sut = maxEarnPerTransaction()

        sut.value shouldBe 50_000L
    }
})
