package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain

@JvmInline
value class GrantedBy(
    val adminId: String,
) {
    init {
        requireDomain(adminId.isNotBlank()) { "지급 관리자 식별자는 비어있을 수 없습니다." }
    }
}
