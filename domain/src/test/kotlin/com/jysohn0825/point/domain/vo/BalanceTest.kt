package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BalanceTest :
    FunSpec({
        test("amount가 0 이상이면 Balance를 생성한다") {
            val result = balance(amount = 100L)

            result.amount shouldBe 100L
        }

        test("amount가 음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { Balance(-1L) }
        }

        test("ZERO는 0원 잔액이다") {
            Balance.ZERO.amount shouldBe 0L
        }

        test("plus 연산은 PointAmount만큼 증가한 새 Balance를 반환한다") {
            val origin = balance(amount = 100L)

            val result = origin + pointAmount(value = 50L)

            result.amount shouldBe 150L
            origin.amount shouldBe 100L
        }

        test("minus 연산은 PointAmount만큼 감소한 새 Balance를 반환한다") {
            val origin = balance(amount = 100L)

            val result = origin - pointAmount(value = 30L)

            result.amount shouldBe 70L
        }

        test("minus 연산 결과가 음수이면 예외가 발생한다") {
            val origin = balance(amount = 10L)

            shouldThrow<IllegalArgumentException> { origin - pointAmount(value = 20L) }
        }
    })
