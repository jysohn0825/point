package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EarningStatusTest :
    BehaviorSpec({
        Given("EarningStatus가 주어지면") {
            When("모든 값을 열거하면") {
                Then("ACTIVE, EXHAUSTED, EXPIRED, CANCELED 네 가지 상태만 존재한다") {
                    EarningStatus.entries.toList() shouldContainExactly
                        listOf(
                            EarningStatus.ACTIVE,
                            EarningStatus.EXHAUSTED,
                            EarningStatus.EXPIRED,
                            EarningStatus.CANCELED,
                        )
                }
            }

            When("ACTIVE 상태에서 isActive를 호출하면") {
                Then("true를 반환한다") {
                    EarningStatus.ACTIVE.isActive() shouldBe true
                }
            }

            When("ACTIVE가 아닌 상태에서 isActive를 호출하면") {
                Then("false를 반환한다") {
                    EarningStatus.EXHAUSTED.isActive() shouldBe false
                    EarningStatus.EXPIRED.isActive() shouldBe false
                    EarningStatus.CANCELED.isActive() shouldBe false
                }
            }
        }
    })
