package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

@JvmInline
value class RemainingAmount(
    val value: BigDecimal,
) {
    init {
        require(value >= BigDecimal.ZERO) { "잔여 포인트는 0 이상이어야 합니다: $value" }
    }

    fun isExhausted(): Boolean = value.signum() == 0

    fun isFullAmountOf(pointAmount: PointAmount): Boolean = value.compareTo(pointAmount.value) == 0

    fun decrease(amount: BigDecimal): RemainingAmount {
        require(amount in BigDecimal.ONE..value) { "차감액($amount)은 1 이상 잔여 포인트($value) 이하이어야 합니다." }
        return RemainingAmount(value - amount)
    }

    fun increase(
        amount: BigDecimal,
        upTo: PointAmount,
    ): RemainingAmount {
        require(amount > BigDecimal.ZERO) { "복원액은 0보다 커야 합니다: $amount" }
        val increased: BigDecimal = value + amount
        require(increased <= upTo.value) { "복원 후 잔여 포인트($increased)는 최초 적립액(${upTo.value})을 초과할 수 없습니다." }
        return RemainingAmount(increased)
    }
}
