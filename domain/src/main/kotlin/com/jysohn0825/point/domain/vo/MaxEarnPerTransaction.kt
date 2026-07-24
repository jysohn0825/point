package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

data class MaxEarnPerTransaction(
    val value: BigDecimal,
) {
    init {
        require(value in MIN..MAX) {
            "1회 적립 가능 포인트는 ${MIN}포인트 이상 ${MAX}포인트 이하이어야 합니다. 입력값: $value"
        }
    }

    companion object {
        val MIN: BigDecimal = BigDecimal.ONE
        val MAX: BigDecimal = BigDecimal(100_000)
    }
}
