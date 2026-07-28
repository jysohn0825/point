package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageCancellationLineEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageLineEntity

class PointUsageMapper {
    companion object {
        /** usage 하나를 라인·취소이력까지 포함해 완전히 조립한다. 조회는 어댑터 책임이고 여기서는 이미 조회된 엔티티만으로 조립한다. */
        fun of(
            entity: PointUsageEntity,
            lineEntities: List<PointUsageLineEntity>,
            cancellationLineEntities: List<PointUsageCancellationLineEntity>,
        ): PointUsage {
            val lineEntityById: Map<String, PointUsageLineEntity> = lineEntities.associateBy { it.id }
            return PointUsage.reconstitute(
                id = entity.id,
                orderNumber = OrderNumber(entity.orderNumber),
                lines = lineEntities.map { of(entity = it) },
                usedAt = entity.usedAt,
                cancellationLines =
                    cancellationLineEntities.map { cancellationLineEntity ->
                        val lineEntity: PointUsageLineEntity =
                            lineEntityById.getValue(cancellationLineEntity.usageLineId)
                        CancellationLine(
                            originalLine = of(entity = lineEntity),
                            restoredAmount = cancellationLineEntity.restoredAmount,
                            restorationType = RestorationType.valueOf(cancellationLineEntity.restoreType),
                        )
                    },
            )
        }

        fun of(
            usage: PointUsage,
            walletId: String,
        ): PointUsageEntity =
            PointUsageEntity(
                id = usage.id,
                walletId = walletId,
                orderNumber = usage.orderNumber.value,
                totalAmount = usage.totalAmount,
                canceledAmount = usage.cancelledAmount,
                status = usage.status.name,
                usedAt = usage.usedAt,
            )

        fun of(
            usageLine: UsageLine,
            usageId: String,
            id: String,
        ): PointUsageLineEntity =
            PointUsageLineEntity(
                id = id,
                usageId = usageId,
                earningId = usageLine.earningId,
                amount = usageLine.amount,
            )

        fun of(entity: PointUsageLineEntity): UsageLine = UsageLine(earningId = entity.earningId, amount = entity.amount)
    }
}
