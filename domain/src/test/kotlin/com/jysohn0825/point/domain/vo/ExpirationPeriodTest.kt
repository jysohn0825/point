package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ExpirationPeriodTest :
    BehaviorSpec({
        Given("ExpirationPeriod의 DEFAULT가 주어지면") {
            When("일수를 조회하면") {
                Then("기본값은 365일이다") {
                    ExpirationPeriod.DEFAULT.days shouldBe 365L
                }
            }
        }

        Given("최소값인 1일이 주어지면") {
            When("ExpirationPeriod를 생성하면") {
                val period: ExpirationPeriod = ExpirationPeriod(ExpirationPeriod.MIN_DAYS)

                Then("정상적으로 생성된다") {
                    period.days shouldBe 1L
                }
            }
        }

        Given("5년 미만의 최대값이 주어지면") {
            When("ExpirationPeriod를 생성하면") {
                val period: ExpirationPeriod = ExpirationPeriod(ExpirationPeriod.MAX_DAYS)

                Then("정상적으로 생성된다") {
                    period.days shouldBe ExpirationPeriod.MAX_DAYS
                }
            }
        }

        Given("0일이 주어지면") {
            When("ExpirationPeriod를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        ExpirationPeriod(0L)
                    }
                }
            }
        }

        Given("5년 이상의 값이 주어지면") {
            When("ExpirationPeriod를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        ExpirationPeriod(365L * 5)
                    }
                }
            }
        }
    })
