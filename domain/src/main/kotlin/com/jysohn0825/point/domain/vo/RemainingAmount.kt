package com.jysohn0825.point.domain.vo

@JvmInline
value class RemainingAmount(
    val value: Long,
) {
    init {
        require(value >= 0) { "잔여 포인트는 0 이상이어야 합니다: $value" }
    }

    fun isExhausted(): Boolean = value == 0L

    fun isFullAmountOf(pointAmount: PointAmount): Boolean = value == pointAmount.value

    fun decrease(amount: Long): RemainingAmount {
        require(amount in 1..value) { "차감액($amount)은 1 이상 잔여 포인트($value) 이하이어야 합니다." }
        return RemainingAmount(value - amount)
    }

    fun increase(
        amount: Long,
        upTo: PointAmount,
    ): RemainingAmount {
        require(amount > 0) { "복원액은 0보다 커야 합니다: $amount" }
        val increased = value + amount
        require(increased <= upTo.value) { "복원 후 잔여 포인트($increased)는 최초 적립액(${upTo.value})을 초과할 수 없습니다." }
        return RemainingAmount(increased)
    }
}
