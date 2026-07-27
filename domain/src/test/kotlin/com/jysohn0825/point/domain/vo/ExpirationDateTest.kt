package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ExpirationDateTest :
    BehaviorSpec({
        Given("적립 시점과 만료 기간이 주어지면") {
            val earnedAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
            val period: ExpirationPeriod = expirationPeriod(10L)

            When("from으로 만료일을 계산하면") {
                val expirationDate: ExpirationDate = ExpirationDate.from(earnedAt = earnedAt, period = period)

                Then("적립 시점에 기간을 더한 시점으로 계산된다") {
                    expirationDate.value shouldBe LocalDateTime.of(2026, 1, 11, 0, 0)
                }
            }
        }

        Given("만료일이 주어지면") {
            val expirationDate: ExpirationDate = ExpirationDate(LocalDateTime.of(2026, 1, 11, 0, 0))

            When("만료일 이전 시점으로 isExpiredAt을 호출하면") {
                Then("만료되지 않은 것으로 판단한다") {
                    expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 10, 0, 0)) shouldBe false
                }
            }

            When("만료일과 동일한 시점으로 isExpiredAt을 호출하면") {
                Then("만료된 것으로 판단한다") {
                    expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 11, 0, 0)) shouldBe true
                }
            }

            When("만료일 이후 시점으로 isExpiredAt을 호출하면") {
                Then("만료된 것으로 판단한다") {
                    expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 12, 0, 0)) shouldBe true
                }
            }
        }
    })
