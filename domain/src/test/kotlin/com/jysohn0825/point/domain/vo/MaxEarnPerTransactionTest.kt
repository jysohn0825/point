package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MaxEarnPerTransactionTest :
    BehaviorSpec({
        Given("하한값(1포인트)이 주어지면") {
            When("MaxEarnPerTransaction을 생성하면") {
                val sut: MaxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal.ONE)

                Then("정상적으로 생성된다") {
                    sut.value shouldBe BigDecimal.ONE
                }
            }
        }

        Given("상한값(10만포인트)이 주어지면") {
            When("MaxEarnPerTransaction을 생성하면") {
                val sut: MaxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal(100_000))

                Then("정상적으로 생성된다") {
                    sut.value shouldBe BigDecimal(100_000)
                }
            }
        }

        Given("1포인트 미만의 값이 주어지면") {
            When("MaxEarnPerTransaction을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        MaxEarnPerTransaction(BigDecimal.ZERO)
                    }
                }
            }
        }

        Given("10만포인트를 초과하는 값이 주어지면") {
            When("MaxEarnPerTransaction을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        MaxEarnPerTransaction(BigDecimal(100_001))
                    }
                }
            }
        }

        Given("픽스처로 생성하면") {
            When("값을 조회하면") {
                val sut: MaxEarnPerTransaction = maxEarnPerTransaction()

                Then("유효한 기본값을 갖는다") {
                    sut.value shouldBe BigDecimal(50_000)
                }
            }
        }
    })
