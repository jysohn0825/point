package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EarnTypeTest :
    BehaviorSpec({
        Given("EarnType이 주어지면") {
            When("모든 값을 열거하면") {
                Then("MANUAL과 SYSTEM 두 값만 존재한다") {
                    EarnType.entries.toList() shouldContainExactly listOf(EarnType.MANUAL, EarnType.SYSTEM)
                }
            }

            When("MANUAL이 SYSTEM보다 우선순위가 있는지 확인하면") {
                Then("true를 반환한다") {
                    EarnType.MANUAL.hasPriorityOver(EarnType.SYSTEM) shouldBe true
                }
            }

            When("SYSTEM이 MANUAL보다 우선순위가 있는지 확인하면") {
                Then("false를 반환한다") {
                    EarnType.SYSTEM.hasPriorityOver(EarnType.MANUAL) shouldBe false
                }
            }

            When("같은 타입끼리 우선순위를 비교하면") {
                Then("false를 반환한다") {
                    EarnType.MANUAL.hasPriorityOver(EarnType.MANUAL) shouldBe false
                    EarnType.SYSTEM.hasPriorityOver(EarnType.SYSTEM) shouldBe false
                }
            }
        }
    })
