package com.jysohn0825.point.infrastructure.persistence.entity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PointWalletHistoryEntityTest :
    BehaviorSpec({
        Given("적립으로 인한 지갑 히스토리를 만들 때") {
            val occurredAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

            When("EARN 타입 히스토리를 생성하면") {
                val entity: PointWalletHistoryEntity =
                    PointWalletHistoryEntity(
                        id = "history-1",
                        walletId = "wallet-1",
                        historyType = "EARN",
                        amount = 1_000L,
                        balanceAfter = 1_000L,
                        earningId = "earning-1",
                        occurredAt = occurredAt,
                    )

                Then("적립 관련 필드는 채워지고 사용/취소 관련 필드는 비어있다") {
                    entity.historyType shouldBe "EARN"
                    entity.earningId shouldBe "earning-1"
                    entity.usageId.shouldBeNull()
                    entity.cancellationId.shouldBeNull()
                    entity.occurredAt shouldBe occurredAt
                }
            }
        }
    })
