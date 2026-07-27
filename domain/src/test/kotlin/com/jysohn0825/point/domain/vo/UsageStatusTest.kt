package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UsageStatusTest :
    BehaviorSpec({
        Given("UsageStatus가 주어지면") {
            When("모든 값을 열거하면") {
                Then("USED, PARTIALLY_CANCELED, FULLY_CANCELED 세 가지 상태만 존재한다") {
                    UsageStatus.values().toList() shouldBe
                        listOf(UsageStatus.USED, UsageStatus.PARTIALLY_CANCELED, UsageStatus.FULLY_CANCELED)
                }
            }

            When("이름으로 값을 조회하면") {
                Then("해당하는 값이 반환된다") {
                    UsageStatus.valueOf("USED") shouldBe UsageStatus.USED
                }
            }
        }
    })
