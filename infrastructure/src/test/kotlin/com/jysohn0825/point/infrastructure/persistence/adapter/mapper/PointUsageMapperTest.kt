package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.fixture.usageLine
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageLineEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.math.BigDecimal

class PointUsageMapperTest :
    BehaviorSpec({
        Given("도메인 사용건이 있을 때") {
            val usage: PointUsage = pointUsage(lines = listOf(usageLine(earningId = "earning-1", amount = BigDecimal(500))))

            When("엔티티로 변환하면") {
                val entity: PointUsageEntity = PointUsageMapper.of(usage = usage, walletId = "wallet-1")

                Then("주문번호와 총액, 상태가 매핑된다") {
                    entity.id shouldBe usage.id
                    entity.walletId shouldBe "wallet-1"
                    entity.orderNumber shouldBe usage.orderNumber.value
                    entity.totalAmount shouldBe usage.totalAmount
                    entity.canceledAmount shouldBe usage.cancelledAmount
                    entity.status shouldBe UsageStatus.USED.name
                }
            }
        }

        Given("사용 라인이 있을 때") {
            val line: UsageLine = usageLine(earningId = "earning-1", amount = BigDecimal(300))

            When("사용 라인 엔티티로 변환하면") {
                val entity: PointUsageLineEntity = PointUsageMapper.of(usageLine = line, usageId = "usage-1")

                Then("식별자가 채번되고 나머지 필드가 매핑된다") {
                    entity.id.shouldNotBeBlank()
                    entity.usageId shouldBe "usage-1"
                    entity.earningId shouldBe "earning-1"
                    entity.amount shouldBe BigDecimal(300)
                }
            }
        }

        Given("사용 라인 엔티티가 있을 때") {
            val entity: PointUsageLineEntity =
                PointUsageLineEntity(id = "line-1", usageId = "usage-1", earningId = "earning-1", amount = BigDecimal(700))

            When("도메인 UsageLine으로 변환하면") {
                val line: UsageLine = PointUsageMapper.of(entity)

                Then("적립건 식별자와 금액이 매핑된다") {
                    line.earningId shouldBe "earning-1"
                    line.amount shouldBe BigDecimal(700)
                }
            }
        }
    })
