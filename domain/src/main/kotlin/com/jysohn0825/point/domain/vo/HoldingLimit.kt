package com.jysohn0825.point.domain.vo

@JvmInline
value class HoldingLimit(
    val value: Long,
) {
    init {
        require(value > 0) { "보유한도는 0보다 커야 합니다." }
    }

    fun canAccept(
        balance: Balance,
        pointAmount: PointAmount,
    ): Boolean = balance.amount + pointAmount.value <= value
}
