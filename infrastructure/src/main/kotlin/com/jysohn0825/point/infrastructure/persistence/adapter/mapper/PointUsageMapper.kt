package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageLineEntity
import java.math.BigDecimal
import java.util.UUID

class PointUsageMapper {
    companion object {
        fun of(
            usage: PointUsage,
            walletId: String,
        ): PointUsageEntity =
            PointUsageEntity(
                id = usage.id,
                walletId = walletId,
                orderNumber = usage.orderNumber.value,
                totalAmount = usage.totalAmount.longValueExact(),
                canceledAmount = usage.cancelledAmount.longValueExact(),
                status = usage.status.name,
                usedAt = usage.usedAt,
            )

        fun of(
            usageLine: UsageLine,
            usageId: String,
        ): PointUsageLineEntity =
            PointUsageLineEntity(
                id = UUID.randomUUID().toString(),
                usageId = usageId,
                earningId = usageLine.earningId,
                amount = usageLine.amount.longValueExact(),
            )

        fun of(entity: PointUsageLineEntity): UsageLine =
            UsageLine(earningId = entity.earningId, amount = BigDecimal.valueOf(entity.amount))
    }
}
