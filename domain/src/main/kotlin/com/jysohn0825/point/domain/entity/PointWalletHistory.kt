package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.event.PointWalletHistoryEvent
import com.jysohn0825.point.domain.event.PointsEarned
import com.jysohn0825.point.domain.event.PointsEarningCancelled
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.event.PointsUsageCancelled
import com.jysohn0825.point.domain.event.PointsUsed
import com.jysohn0825.point.domain.exception.requireDomain
import com.jysohn0825.point.domain.vo.HistoryType
import java.math.BigDecimal
import java.time.LocalDateTime

class PointWalletHistory private constructor(
    val id: String,
    val historyType: HistoryType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val earningId: String?,
    val usageId: String?,
    val cancellationId: String?,
    val occurredAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointWalletHistory) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun record(
            historyType: HistoryType,
            amount: BigDecimal,
            balanceAfter: BigDecimal,
            id: String,
            earningId: String? = null,
            usageId: String? = null,
            cancellationId: String? = null,
            occurredAt: LocalDateTime = LocalDateTime.now(),
        ): PointWalletHistory {
            requireDomain(balanceAfter >= BigDecimal.ZERO) { "기록 시점 잔액은 0보다 작을 수 없습니다: $balanceAfter" }
            requireDomain(listOfNotNull(earningId, usageId, cancellationId).size == 1) {
                "히스토리는 적립/사용/취소 참조 중 정확히 하나만 가져야 합니다."
            }
            return PointWalletHistory(
                id = id,
                historyType = historyType,
                amount = amount,
                balanceAfter = balanceAfter,
                earningId = earningId,
                usageId = usageId,
                cancellationId = cancellationId,
                occurredAt = occurredAt,
            )
        }

        /** [PointWalletHistoryEvent] 각 타입을 어떤 [HistoryType]·참조ID로 기록할지는 도메인이 전담한다. */
        fun from(
            event: PointWalletHistoryEvent,
            id: String,
            occurredAt: LocalDateTime = LocalDateTime.now(),
        ): PointWalletHistory =
            when (event) {
                is PointsEarned ->
                    record(
                        historyType = HistoryType.EARN,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        id = id,
                        earningId = event.earningId,
                        occurredAt = occurredAt,
                    )

                is PointsEarningCancelled ->
                    record(
                        historyType = HistoryType.EARN_CANCEL,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        id = id,
                        earningId = event.earningId,
                        occurredAt = occurredAt,
                    )

                is PointsUsed ->
                    record(
                        historyType = HistoryType.USE,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        id = id,
                        usageId = event.usageId,
                        occurredAt = occurredAt,
                    )

                is PointsUsageCancelled ->
                    record(
                        historyType = HistoryType.USE_CANCEL,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        id = id,
                        cancellationId = event.cancellationId,
                        occurredAt = occurredAt,
                    )

                is PointsExpired ->
                    record(
                        historyType = HistoryType.EXPIRE,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        id = id,
                        earningId = event.earningId,
                        occurredAt = occurredAt,
                    )
            }
    }
}
