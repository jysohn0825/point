package com.jysohn0825.point.domain.vo

data class UsageLine(
    val earningId: EarningId,
    val amount: Long,
) {
    init {
        require(amount > 0) { "차감액은 0보다 커야 합니다: $amount" }
    }
}
