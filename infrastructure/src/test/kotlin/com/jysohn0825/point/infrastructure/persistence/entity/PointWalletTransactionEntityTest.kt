package com.jysohn0825.point.infrastructure.persistence.entity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PointWalletTransactionEntityTest :
    BehaviorSpec({
        Given("적립으로 인한 지갑 거래 내역을 만들 때") {
            val occurredAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

            When("EARN 타입 거래를 생성하면") {
                val entity: PointWalletTransactionEntity =
                    PointWalletTransactionEntity(
                        id = "tx-1",
                        walletId = "wallet-1",
                        transactionType = "EARN",
                        amount = 1_000L,
                        balanceAfter = 1_000L,
                        earningId = "earning-1",
                        occurredAt = occurredAt,
                    )

                Then("적립 관련 필드는 채워지고 사용/취소 관련 필드는 비어있다") {
                    entity.transactionType shouldBe "EARN"
                    entity.earningId shouldBe "earning-1"
                    entity.usageId.shouldBeNull()
                    entity.cancellationId.shouldBeNull()
                    entity.occurredAt shouldBe occurredAt
                }
            }
        }
    })
