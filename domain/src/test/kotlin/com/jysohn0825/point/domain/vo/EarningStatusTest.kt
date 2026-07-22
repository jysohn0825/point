package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class EarningStatusTest :
    FunSpec({
        test("ACTIVE, EXHAUSTED, EXPIRED, CANCELED 네 가지 상태만 존재한다") {
            EarningStatus.entries.toList() shouldContainExactly
                listOf(
                    EarningStatus.ACTIVE,
                    EarningStatus.EXHAUSTED,
                    EarningStatus.EXPIRED,
                    EarningStatus.CANCELED,
                )
        }

        context("isActive") {
            test("ACTIVE 상태면 true를 반환한다") {
                EarningStatus.ACTIVE.isActive() shouldBe true
            }

            test("ACTIVE가 아닌 상태면 false를 반환한다") {
                EarningStatus.EXHAUSTED.isActive() shouldBe false
                EarningStatus.EXPIRED.isActive() shouldBe false
                EarningStatus.CANCELED.isActive() shouldBe false
            }
        }
    })
