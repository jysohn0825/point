package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import java.time.LocalDate
import java.util.UUID

class PointPolicyMapper {
    companion object {
        fun of(entity: PointPolicyEntity): PointPolicy =
            PointPolicy(
                id = entity.id,
                maxEarnPerTransaction = MaxEarnPerTransaction(entity.maxEarnPerTransaction),
                maxHoldingAmount = MaxHoldingAmount(entity.maxHoldingAmount),
                defaultExpirationPeriod = ExpirationPeriod(entity.defaultExpirationDays.toLong()),
            )

        fun of(
            policy: PointPolicy,
            policyVersion: Int,
            appliedAt: LocalDate,
            createdByAdminId: String,
        ): PointPolicyEntity =
            PointPolicyEntity(
                id = UUID.randomUUID().toString(),
                policyVersion = policyVersion,
                maxEarnPerTransaction = policy.maxEarnPerTransaction.value,
                maxHoldingAmount = policy.maxHoldingAmount.value,
                defaultExpirationDays = policy.defaultExpirationPeriod.days.toInt(),
                appliedAt = appliedAt,
                createdByAdminId = createdByAdminId,
            )
    }
}
