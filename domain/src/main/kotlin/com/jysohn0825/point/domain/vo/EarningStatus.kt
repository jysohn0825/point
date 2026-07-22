package com.jysohn0825.point.domain.vo

enum class EarningStatus {
    ACTIVE,
    EXHAUSTED,
    EXPIRED,
    CANCELED,
    ;

    fun isActive(): Boolean = this == ACTIVE
}
