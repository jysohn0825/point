package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletHistoryEntity

class PointWalletHistoryMapper {
    companion object {
        fun of(
            history: PointWalletHistory,
            walletId: String,
        ): PointWalletHistoryEntity =
            PointWalletHistoryEntity(
                id = history.id,
                walletId = walletId,
                historyType = history.historyType.name,
                amount = history.amount.longValueExact(),
                balanceAfter = history.balanceAfter.longValueExact(),
                earningId = history.earningId,
                usageId = history.usageId,
                cancellationId = history.cancellationId,
                occurredAt = history.occurredAt,
            )
    }
}
