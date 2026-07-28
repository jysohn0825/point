package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointExpirationAllocatorTest :
    BehaviorSpec({
        val sut: PointExpirationAllocator = DefaultPointExpirationAllocator()

        given("만료 대상 적립건이 여러 건일 때") {
            val first: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(1_000)))
            val second: PointEarning = pointEarning(id = "e2", amount = pointAmount(BigDecimal(500)))
            second.use(BigDecimal(200))

            `when`("만료 처리를 배분하면") {
                val totalExpired: PointAmount = sut.allocate(dueEarnings = listOf(first, second))

                then("각 적립건은 EXPIRED 상태가 되고, 만료 전 잔여금액의 합계를 반환한다") {
                    first.status shouldBe EarningStatus.EXPIRED
                    second.status shouldBe EarningStatus.EXPIRED
                    totalExpired.value shouldBe BigDecimal(1_300)
                }
            }
        }

        given("만료 대상 적립건이 하나일 때") {
            val earning: PointEarning = pointEarning(id = "e1", amount = pointAmount(BigDecimal(300)))

            `when`("만료 처리를 배분하면") {
                val totalExpired: PointAmount = sut.allocate(dueEarnings = listOf(earning))

                then("잔여금액 전액이 만료 금액으로 반환된다") {
                    earning.status shouldBe EarningStatus.EXPIRED
                    totalExpired.value shouldBe BigDecimal(300)
                }
            }
        }
    })
