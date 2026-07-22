package com.jysohn0825.point.domain.vo

@JvmInline
value class PolicyVersion(
    val value: Long,
) {
    init {
        require(value > 0) { "정책 버전은 0보다 커야 합니다: $value" }
    }
}
