package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

data class EarningUsageTrace(
    val orderNumber: OrderNumber,
    val amount: BigDecimal,
) {
    init {
        require(amount > BigDecimal.ZERO) { "차감액은 0보다 커야 합니다: $amount" }
    }
}
