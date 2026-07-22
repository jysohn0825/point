package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.PolicyVersion
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.domain.vo.policyVersion as defaultPolicyVersion
import java.time.LocalDateTime
import java.util.UUID

val POINT_EARNING_DEFAULT_EARNED_AT: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

fun pointEarning(
    id: String = UUID.randomUUID().toString(),
    amount: PointAmount = pointAmount(),
    earnType: EarnType = EarnType.SYSTEM,
    grantedBy: GrantedBy? = null,
    earnedAt: LocalDateTime = POINT_EARNING_DEFAULT_EARNED_AT,
    period: ExpirationPeriod = expirationPeriod(),
    policyVersion: PolicyVersion = defaultPolicyVersion(),
): PointEarning = PointEarning.earn(
    id = id,
    amount = amount,
    earnType = earnType,
    grantedBy = grantedBy,
    earnedAt = earnedAt,
    period = period,
    policyVersion = policyVersion,
)
