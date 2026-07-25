package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

@JvmInline
value class Balance(
    val amount: BigDecimal,
) {
    init {
        requireDomain(amount >= BigDecimal.ZERO) { "잔액은 0보다 작을 수 없습니다." }
    }

    operator fun plus(pointAmount: PointAmount): Balance = Balance(amount + pointAmount.value)

    operator fun minus(pointAmount: PointAmount): Balance = Balance(amount - pointAmount.value)

    companion object {
        val ZERO: Balance = Balance(BigDecimal.ZERO)
    }
}
