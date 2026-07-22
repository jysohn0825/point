package com.jysohn0825.point.domain.vo

@JvmInline
value class PointAmount(val value: Long) {
    init {
        require(value > 0) { "적립액은 0보다 커야 합니다: $value" }
    }
}
