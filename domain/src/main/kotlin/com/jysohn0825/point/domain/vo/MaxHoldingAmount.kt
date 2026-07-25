package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

data class MaxHoldingAmount(
    val value: BigDecimal,
) {
    init {
        requireDomain(value >= MIN) {
            "개인별 보유 가능한 무료 포인트의 최대 금액은 ${MIN}포인트 이상이어야 합니다. 입력값: $value"
        }
    }

    companion object {
        val MIN: BigDecimal = BigDecimal.ONE
    }
}
