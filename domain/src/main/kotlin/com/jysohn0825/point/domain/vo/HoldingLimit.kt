package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

@JvmInline
value class HoldingLimit(
    val value: BigDecimal,
) {
    init {
        requireDomain(value > BigDecimal.ZERO) { "보유한도는 0보다 커야 합니다." }
    }

    fun canAccept(
        balance: Balance,
        pointAmount: PointAmount,
    ): Boolean = balance.amount + pointAmount.value <= value
}
