package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MaxEarnPerTransactionTest :
    FunSpec({

        test("하한값(1포인트)으로 생성할 수 있다") {
            val sut: MaxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal.ONE)

            sut.value shouldBe BigDecimal.ONE
        }

        test("상한값(10만포인트)으로 생성할 수 있다") {
            val sut: MaxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal(100_000))

            sut.value shouldBe BigDecimal(100_000)
        }

        test("1포인트 미만이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                MaxEarnPerTransaction(BigDecimal.ZERO)
            }
        }

        test("10만포인트를 초과하면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                MaxEarnPerTransaction(BigDecimal(100_001))
            }
        }

        test("픽스처는 유효한 기본값으로 생성된다") {
            val sut: MaxEarnPerTransaction = maxEarnPerTransaction()

            sut.value shouldBe BigDecimal(50_000)
        }
    })
