package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class BalanceTest :
    FunSpec({
        test("amount가 0 이상이면 Balance를 생성한다") {
            val result: Balance = balance(amount = BigDecimal(100))

            result.amount shouldBe BigDecimal(100)
        }

        test("amount가 음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> { Balance(BigDecimal(-1)) }
        }

        test("ZERO는 0원 잔액이다") {
            Balance.ZERO.amount shouldBe BigDecimal.ZERO
        }

        test("plus 연산은 PointAmount만큼 증가한 새 Balance를 반환한다") {
            val origin: Balance = balance(amount = BigDecimal(100))

            val result: Balance = origin + pointAmount(value = BigDecimal(50))

            result.amount shouldBe BigDecimal(150)
            origin.amount shouldBe BigDecimal(100)
        }

        test("minus 연산은 PointAmount만큼 감소한 새 Balance를 반환한다") {
            val origin: Balance = balance(amount = BigDecimal(100))

            val result: Balance = origin - pointAmount(value = BigDecimal(30))

            result.amount shouldBe BigDecimal(70)
        }

        test("minus 연산 결과가 음수이면 예외가 발생한다") {
            val origin: Balance = balance(amount = BigDecimal(10))

            shouldThrow<IllegalArgumentException> { origin - pointAmount(value = BigDecimal(20)) }
        }
    })
