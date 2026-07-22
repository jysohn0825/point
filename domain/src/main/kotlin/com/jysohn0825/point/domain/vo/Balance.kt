package com.jysohn0825.point.domain.vo

@JvmInline
value class Balance(
    val amount: Long,
) {
    init {
        require(amount >= 0) { "잔액은 0보다 작을 수 없습니다." }
    }

    operator fun plus(pointAmount: PointAmount): Balance = Balance(amount + pointAmount.value)

    operator fun minus(pointAmount: PointAmount): Balance = Balance(amount - pointAmount.value)

    companion object {
        val ZERO = Balance(0)
    }
}
