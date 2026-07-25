package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExpirationPeriodTest :
    FunSpec({
        test("기본값은 365일이다") {
            ExpirationPeriod.DEFAULT.days shouldBe 365L
        }

        test("최소값인 1일을 허용한다") {
            val period: ExpirationPeriod = ExpirationPeriod(ExpirationPeriod.MIN_DAYS)

            period.days shouldBe 1L
        }

        test("5년 미만의 최대값을 허용한다") {
            val period: ExpirationPeriod = ExpirationPeriod(ExpirationPeriod.MAX_DAYS)

            period.days shouldBe ExpirationPeriod.MAX_DAYS
        }

        test("0일이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                ExpirationPeriod(0L)
            }
        }

        test("5년 이상이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                ExpirationPeriod(365L * 5)
            }
        }
    })
