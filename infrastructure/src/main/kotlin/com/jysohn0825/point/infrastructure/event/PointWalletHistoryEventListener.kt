package com.jysohn0825.point.infrastructure.event

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.event.PointWalletHistoryEvent
import com.jysohn0825.point.domain.repository.PointWalletHistoryRepository
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * [PointWalletHistoryEvent]를 구독해 히스토리를 영속화한다. `@EventListener`는 기본적으로 발행 호출과
 * 같은 스레드·같은 트랜잭션 안에서 동기 실행되므로, 지갑/원장 저장과 히스토리 저장이 같은 트랜잭션에서 커밋·롤백된다.
 */
@Component
class PointWalletHistoryEventListener(
    private val historyRepository: PointWalletHistoryRepository,
    private val keyGenerator: DistributedKeyGenerator,
) {
    @EventListener
    fun on(event: PointWalletHistoryEvent) {
        val history: PointWalletHistory =
            PointWalletHistory.from(event = event, id = keyGenerator.next(HISTORY_KEY_NAME).toString())
        historyRepository.save(history = history, walletId = event.walletId)
    }

    companion object {
        private const val HISTORY_KEY_NAME: String = "point-wallet-history"
    }
}
