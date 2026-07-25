package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ExpirationDateTest :
    FunSpec({
        context("from") {
            test("적립 시점에 기간을 더한 시점으로 계산된다") {
                val earnedAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
                val period: ExpirationPeriod = expirationPeriod(10L)

                val expirationDate: ExpirationDate = ExpirationDate.from(earnedAt = earnedAt, period = period)

                expirationDate.value shouldBe LocalDateTime.of(2026, 1, 11, 0, 0)
            }
        }

        context("isExpiredAt") {
            val expirationDate: ExpirationDate = ExpirationDate(LocalDateTime.of(2026, 1, 11, 0, 0))

            test("만료일 이전이면 만료되지 않은 것으로 판단한다") {
                expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 10, 0, 0)) shouldBe false
            }

            test("만료일과 동일한 시점이면 만료된 것으로 판단한다") {
                expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 11, 0, 0)) shouldBe true
            }

            test("만료일 이후이면 만료된 것으로 판단한다") {
                expirationDate.isExpiredAt(LocalDateTime.of(2026, 1, 12, 0, 0)) shouldBe true
            }
        }
    })
