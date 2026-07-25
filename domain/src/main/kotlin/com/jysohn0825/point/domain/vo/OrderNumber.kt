package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain

@JvmInline
value class OrderNumber(
    val value: String,
) {
    init {
        requireDomain(value.isNotBlank()) { "주문번호는 공백일 수 없습니다." }
    }
}
