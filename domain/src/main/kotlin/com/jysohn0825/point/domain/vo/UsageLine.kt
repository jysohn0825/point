package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

data class UsageLine(
    val earningId: String,
    val amount: BigDecimal,
) {
    init {
        requireDomain(amount > BigDecimal.ZERO) { "차감액은 0보다 커야 합니다: $amount" }
    }
}
