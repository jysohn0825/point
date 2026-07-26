package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.entity.pointWalletHistory
import com.jysohn0825.point.domain.vo.HistoryType
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletHistoryEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class PointWalletHistoryMapperTest :
    BehaviorSpec({
        Given("사용으로 인한 지갑 히스토리 도메인 객체가 있을 때") {
            val history: PointWalletHistory =
                pointWalletHistory(
                    id = "history-1",
                    historyType = HistoryType.USE,
                    amount = BigDecimal(-300),
                    balanceAfter = BigDecimal(700),
                    earningId = null,
                    usageId = "usage-1",
                    occurredAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                )

            When("엔티티로 변환하면") {
                val entity: PointWalletHistoryEntity = PointWalletHistoryMapper.of(history = history, walletId = "wallet-1")

                Then("모든 필드가 그대로 매핑된다") {
                    entity.id shouldBe "history-1"
                    entity.walletId shouldBe "wallet-1"
                    entity.historyType shouldBe "USE"
                    entity.amount shouldBe -300L
                    entity.balanceAfter shouldBe 700L
                    entity.earningId.shouldBeNull()
                    entity.usageId shouldBe "usage-1"
                    entity.cancellationId.shouldBeNull()
                    entity.occurredAt shouldBe LocalDateTime.of(2026, 1, 1, 0, 0)
                }
            }
        }
    })
