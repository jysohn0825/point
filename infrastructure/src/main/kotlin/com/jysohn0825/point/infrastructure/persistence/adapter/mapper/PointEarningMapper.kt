package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.ExpirationDate
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.RemainingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import java.time.LocalDateTime

class PointEarningMapper {
    companion object {
        fun of(entity: PointEarningEntity): PointEarning =
            PointEarning.reconstitute(
                id = entity.id,
                amount = PointAmount(entity.amount),
                earnType = EarnType.valueOf(entity.earnType),
                sourceReferenceId = entity.sourceReferenceId,
                grantedBy = entity.grantedByAdminId?.let { GrantedBy(it) },
                earnedAt = entity.earnedAt,
                expirationDate = ExpirationDate(entity.expiresAt),
                remainingAmount = RemainingAmount(entity.remainingAmount),
                status = EarningStatus.valueOf(entity.status),
            )

        fun of(
            earning: PointEarning,
            walletId: String,
            policyId: String,
            existing: PointEarningEntity?,
        ): PointEarningEntity =
            PointEarningEntity(
                id = earning.id,
                walletId = walletId,
                policyId = policyId,
                amount = earning.amount.value,
                remainingAmount = earning.remainingAmount.value,
                earnType = earning.earnType.name,
                sourceReferenceId = earning.sourceReferenceId,
                grantedByAdminId = earning.grantedBy?.adminId,
                earnedAt = earning.earnedAt,
                expiresAt = earning.expirationDate.value,
                status = earning.status.name,
                // canceledAt은 도메인이 들고 있지 않은 컬럼이라, CANCELED로 새로 전이하는 순간에만 now()로 채우고
                // 그 외에는 기존 값을 보존한다.
                canceledAt =
                    when {
                        earning.status == EarningStatus.CANCELED && existing?.status != EarningStatus.CANCELED.name -> LocalDateTime.now()
                        else -> existing?.canceledAt
                    },
            )
    }
}
