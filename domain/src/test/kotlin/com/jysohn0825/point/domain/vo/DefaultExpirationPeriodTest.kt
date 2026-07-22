package com.jysohn0825.point.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DefaultExpirationPeriodTest :
    FunSpec({

        test("하한값(1일)으로 생성할 수 있다") {
            val sut = DefaultExpirationPeriod(1)

            sut.days shouldBe 1
        }

        test("상한값 미만(5년-1일)으로 생성할 수 있다") {
            val sut = DefaultExpirationPeriod(DefaultExpirationPeriod.MAX_DAYS - 1)

            sut.days shouldBe DefaultExpirationPeriod.MAX_DAYS - 1
        }

        test("1일 미만이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                DefaultExpirationPeriod(0)
            }
        }

        test("5년 이상이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                DefaultExpirationPeriod(DefaultExpirationPeriod.MAX_DAYS)
            }
        }

        test("default()는 365일을 반환한다") {
            val sut = DefaultExpirationPeriod.default()

            sut.days shouldBe 365
        }

        test("픽스처는 유효한 기본값(365일)으로 생성된다") {
            val sut = defaultExpirationPeriod()

            sut.days shouldBe 365
        }
    })
