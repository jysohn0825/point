package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.event.PointsEarned
import com.jysohn0825.point.domain.event.PointsEarningCancelled
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.event.PointsUsageCancelled
import com.jysohn0825.point.domain.event.PointsUsed
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.HistoryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointWalletHistoryTest :
    BehaviorSpec({
        Given("적립·사용·취소 참조 중 정확히 하나만 채워서 기록하면") {
            When("record()를 호출하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.record(
                        historyType = HistoryType.EARN,
                        amount = BigDecimal(1_000),
                        balanceAfter = BigDecimal(1_000),
                        earningId = "earning-1",
                    )

                Then("정상적으로 생성된다") {
                    history.historyType shouldBe HistoryType.EARN
                    history.earningId shouldBe "earning-1"
                    history.usageId shouldBe null
                    history.cancellationId shouldBe null
                }
            }
        }

        Given("참조 값이 하나도 없거나 두 개 이상 채워진 경우") {
            When("record()를 호출하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        PointWalletHistory.record(
                            historyType = HistoryType.EARN,
                            amount = BigDecimal(1_000),
                            balanceAfter = BigDecimal(1_000),
                        )
                    }
                    shouldThrow<PointDomainException> {
                        PointWalletHistory.record(
                            historyType = HistoryType.USE,
                            amount = BigDecimal(-1_000),
                            balanceAfter = BigDecimal(1_000),
                            earningId = "earning-1",
                            usageId = "usage-1",
                        )
                    }
                }
            }
        }

        Given("기록 시점 잔액이 음수인 경우") {
            When("record()를 호출하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        PointWalletHistory.record(
                            historyType = HistoryType.USE,
                            amount = BigDecimal(-1_000),
                            balanceAfter = BigDecimal(-1),
                            usageId = "usage-1",
                        )
                    }
                }
            }
        }

        Given("PointsEarned 이벤트가 발생했을 때") {
            When("from()으로 변환하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.from(
                        event =
                            PointsEarned(
                                walletId = "wallet-1",
                                amount = BigDecimal(1_000),
                                balanceAfter = BigDecimal(1_000),
                                earningId = "earning-1",
                            ),
                        id = "history-1",
                    )

                Then("EARN 타입으로, earningId를 참조로 기록된다") {
                    history.id shouldBe "history-1"
                    history.historyType shouldBe HistoryType.EARN
                    history.amount shouldBe BigDecimal(1_000)
                    history.earningId shouldBe "earning-1"
                }
            }
        }

        Given("PointsEarningCancelled 이벤트가 발생했을 때") {
            When("from()으로 변환하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.from(
                        event =
                            PointsEarningCancelled(
                                walletId = "wallet-1",
                                amount = BigDecimal(-1_000),
                                balanceAfter = BigDecimal.ZERO,
                                earningId = "earning-1",
                            ),
                        id = "history-2",
                    )

                Then("EARN_CANCEL 타입으로, earningId를 참조로 기록된다") {
                    history.historyType shouldBe HistoryType.EARN_CANCEL
                    history.amount shouldBe BigDecimal(-1_000)
                    history.earningId shouldBe "earning-1"
                }
            }
        }

        Given("PointsUsed 이벤트가 발생했을 때") {
            When("from()으로 변환하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.from(
                        event =
                            PointsUsed(
                                walletId = "wallet-1",
                                amount = BigDecimal(-300),
                                balanceAfter = BigDecimal(700),
                                usageId = "usage-1",
                            ),
                        id = "history-3",
                    )

                Then("USE 타입으로, usageId를 참조로 기록된다") {
                    history.historyType shouldBe HistoryType.USE
                    history.usageId shouldBe "usage-1"
                }
            }
        }

        Given("PointsUsageCancelled 이벤트가 발생했을 때") {
            When("from()으로 변환하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.from(
                        event =
                            PointsUsageCancelled(
                                walletId = "wallet-1",
                                amount = BigDecimal(300),
                                balanceAfter = BigDecimal(1_000),
                                cancellationId = "cancellation-1",
                            ),
                        id = "history-4",
                    )

                Then("USE_CANCEL 타입으로, cancellationId를 참조로 기록된다") {
                    history.historyType shouldBe HistoryType.USE_CANCEL
                    history.cancellationId shouldBe "cancellation-1"
                }
            }
        }

        Given("PointsExpired 이벤트가 발생했을 때") {
            When("from()으로 변환하면") {
                val history: PointWalletHistory =
                    PointWalletHistory.from(
                        event =
                            PointsExpired(
                                walletId = "wallet-1",
                                amount = BigDecimal(-500),
                                balanceAfter = BigDecimal(500),
                                earningId = "earning-2",
                            ),
                        id = "history-5",
                    )

                Then("EXPIRE 타입으로, earningId를 참조로 기록된다") {
                    history.historyType shouldBe HistoryType.EXPIRE
                    history.earningId shouldBe "earning-2"
                }
            }
        }
    })
