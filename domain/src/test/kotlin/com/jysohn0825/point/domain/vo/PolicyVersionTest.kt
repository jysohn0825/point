package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyVersionTest : FunSpec({
    test("양수 값이면 정상적으로 생성된다") {
        val policyVersion = PolicyVersion(3L)

        policyVersion.value shouldBe 3L
    }

    test("0이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            PolicyVersion(0L)
        }
    }

    test("음수이면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            PolicyVersion(-1L)
        }
    }
})
