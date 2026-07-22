package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.DefaultExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import com.jysohn0825.point.domain.vo.defaultExpirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount

fun pointPolicy(
    id: String = "point-policy-1",
    maxEarnPerTransaction: MaxEarnPerTransaction = maxEarnPerTransaction(),
    maxHoldingAmount: MaxHoldingAmount = maxHoldingAmount(),
    defaultExpirationPeriod: DefaultExpirationPeriod = defaultExpirationPeriod(),
): PointPolicy =
    PointPolicy(
        id = id,
        maxEarnPerTransaction = maxEarnPerTransaction,
        maxHoldingAmount = maxHoldingAmount,
        defaultExpirationPeriod = defaultExpirationPeriod,
    )
