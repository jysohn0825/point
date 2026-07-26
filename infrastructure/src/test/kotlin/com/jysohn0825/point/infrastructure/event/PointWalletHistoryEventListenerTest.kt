package com.jysohn0825.point.infrastructure.event

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.event.PointsEarned
import com.jysohn0825.point.domain.event.PointsUsed
import com.jysohn0825.point.domain.repository.FakePointWalletHistoryRepository
import com.jysohn0825.point.domain.vo.HistoryType
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicLong

private class FakeListenerKeyGenerator : DistributedKeyGenerator {
    private val counter: AtomicLong = AtomicLong()

    override fun next(name: String): Long = counter.incrementAndGet()
}

class PointWalletHistoryEventListenerTest :
    BehaviorSpec({
        Given("PointsEarned 이벤트가 발행되면") {
            val historyRepository: FakePointWalletHistoryRepository = FakePointWalletHistoryRepository()
            val listener: PointWalletHistoryEventListener =
                PointWalletHistoryEventListener(historyRepository = historyRepository, keyGenerator = FakeListenerKeyGenerator())

            When("리스너가 이벤트를 받으면") {
                listener.on(
                    PointsEarned(
                        walletId = "wallet-1",
                        amount = BigDecimal(1_000),
                        balanceAfter = BigDecimal(1_000),
                        earningId = "earning-1",
                    ),
                )

                Then("EARN 타입 히스토리가 저장된다") {
                    historyRepository.savedHistories.size shouldBe 1
                    val saved: PointWalletHistory = historyRepository.savedHistories[0]
                    saved.historyType shouldBe HistoryType.EARN
                    saved.amount shouldBe BigDecimal(1_000)
                    saved.earningId shouldBe "earning-1"
                    historyRepository.findAllByWalletId("wallet-1").size shouldBe 1
                }
            }
        }

        Given("서로 다른 지갑에서 이벤트가 여러 건 발행되면") {
            val historyRepository: FakePointWalletHistoryRepository = FakePointWalletHistoryRepository()
            val listener: PointWalletHistoryEventListener =
                PointWalletHistoryEventListener(historyRepository = historyRepository, keyGenerator = FakeListenerKeyGenerator())

            When("리스너가 순서대로 처리하면") {
                listener.on(
                    PointsEarned(
                        walletId = "wallet-1",
                        amount = BigDecimal(1_000),
                        balanceAfter = BigDecimal(1_000),
                        earningId = "earning-1",
                    ),
                )
                listener.on(
                    PointsUsed(walletId = "wallet-2", amount = BigDecimal(-300), balanceAfter = BigDecimal(700), usageId = "usage-1"),
                )

                Then("각 히스토리가 서로 다른 id로 저장된다") {
                    val saved: List<PointWalletHistory> = historyRepository.savedHistories
                    saved.size shouldBe 2
                    saved.map { it.id }.toSet().size shouldBe 2
                }
            }
        }
    })
