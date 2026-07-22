package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EarnTypeTest :
    FunSpec({
        test("MANUAL과 SYSTEM 두 값만 존재한다") {
            EarnType.entries.toList() shouldContainExactly listOf(EarnType.MANUAL, EarnType.SYSTEM)
        }

        context("hasPriorityOver") {
            test("MANUAL은 SYSTEM보다 우선순위가 높다") {
                EarnType.MANUAL.hasPriorityOver(EarnType.SYSTEM) shouldBe true
            }

            test("SYSTEM은 MANUAL보다 우선순위가 높지 않다") {
                EarnType.SYSTEM.hasPriorityOver(EarnType.MANUAL) shouldBe false
            }

            test("같은 타입끼리는 우선순위가 없다") {
                EarnType.MANUAL.hasPriorityOver(EarnType.MANUAL) shouldBe false
                EarnType.SYSTEM.hasPriorityOver(EarnType.SYSTEM) shouldBe false
            }
        }
    })
