package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class PointPolicyMapper {
    companion object {
        fun of(entity: PointPolicyEntity): PointPolicy =
            PointPolicy(
                id = entity.id,
                maxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal.valueOf(entity.maxEarnPerTransaction.toLong())),
                maxHoldingAmount = MaxHoldingAmount(BigDecimal.valueOf(entity.maxHoldingAmount)),
                defaultExpirationPeriod = ExpirationPeriod(entity.defaultExpirationDays.toLong()),
            )

        fun of(
            policy: PointPolicy,
            policyVersion: Int,
            appliedAt: LocalDateTime,
            createdByAdminId: String,
        ): PointPolicyEntity =
            PointPolicyEntity(
                id = UUID.randomUUID().toString(),
                policyVersion = policyVersion,
                maxEarnPerTransaction = policy.maxEarnPerTransaction.value.intValueExact(),
                maxHoldingAmount = policy.maxHoldingAmount.value.longValueExact(),
                defaultExpirationDays = policy.defaultExpirationPeriod.days.toInt(),
                appliedAt = appliedAt,
                createdByAdminId = createdByAdminId,
            )
    }
}
