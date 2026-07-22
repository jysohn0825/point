package com.jysohn0825.point.domain.vo

@JvmInline
value class OrderNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "주문번호는 공백일 수 없습니다." }
    }
}
