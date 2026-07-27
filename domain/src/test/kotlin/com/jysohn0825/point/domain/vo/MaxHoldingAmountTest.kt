package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MaxHoldingAmountTest :
    BehaviorSpec({
        Given("하한값(1포인트)이 주어지면") {
            When("MaxHoldingAmount를 생성하면") {
                val sut: MaxHoldingAmount = MaxHoldingAmount(BigDecimal.ONE)

                Then("정상적으로 생성된다") {
                    sut.value shouldBe BigDecimal.ONE
                }
            }
        }

        Given("1포인트 미만의 값이 주어지면") {
            When("MaxHoldingAmount를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        MaxHoldingAmount(BigDecimal.ZERO)
                    }
                }
            }
        }

        Given("픽스처로 생성하면") {
            When("값을 조회하면") {
                val sut: MaxHoldingAmount = maxHoldingAmount()

                Then("유효한 기본값을 갖는다") {
                    sut.value shouldBe BigDecimal(1_000_000)
                }
            }
        }
    })
