package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class OrderNumberTest :
    BehaviorSpec({
        Given("공백이 아닌 값이 주어지면") {
            When("OrderNumber를 생성하면") {
                val orderNumber: OrderNumber = OrderNumber("ORDER-20260722-0001")

                Then("정상적으로 생성된다") {
                    orderNumber.value shouldBe "ORDER-20260722-0001"
                }
            }
        }

        Given("공백 문자열이 주어지면") {
            When("OrderNumber를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { OrderNumber("   ") }
                }
            }
        }

        Given("빈 문자열이 주어지면") {
            When("OrderNumber를 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { OrderNumber("") }
                }
            }
        }

        Given("같은 값을 가진 두 OrderNumber가 주어지면") {
            When("동등성을 비교하면") {
                Then("서로 동등하다") {
                    (OrderNumber("ORDER-1") == OrderNumber("ORDER-1")) shouldBe true
                }
            }

            When("hashCode를 비교하면") {
                Then("동일하다") {
                    OrderNumber("ORDER-1").hashCode() shouldBe OrderNumber("ORDER-1").hashCode()
                }
            }
        }

        Given("다른 값을 가진 두 OrderNumber가 주어지면") {
            When("동등성을 비교하면") {
                Then("서로 동등하지 않다") {
                    (OrderNumber("ORDER-1") == OrderNumber("ORDER-2")) shouldBe false
                }
            }
        }
    })
