package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount

fun pointPolicy(
    id: String = "point-policy-1",
    maxEarnPerTransaction: MaxEarnPerTransaction = maxEarnPerTransaction(),
    maxHoldingAmount: MaxHoldingAmount = maxHoldingAmount(),
    defaultExpirationPeriod: ExpirationPeriod = expirationPeriod(),
): PointPolicy =
    PointPolicy(
        id = id,
        maxEarnPerTransaction = maxEarnPerTransaction,
        maxHoldingAmount = maxHoldingAmount,
        defaultExpirationPeriod = defaultExpirationPeriod,
    )
