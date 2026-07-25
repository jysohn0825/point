package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

@JvmInline
value class PointAmount(
    val value: BigDecimal,
) {
    init {
        requireDomain(value > BigDecimal.ZERO) { "적립액은 0보다 커야 합니다: $value" }
    }
}
