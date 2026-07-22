package com.jysohn0825.point.domain.vo

data class MaxHoldingAmount(
    val value: Long,
) {
    init {
        require(value >= MIN) {
            "개인별 보유 가능한 무료 포인트의 최대 금액은 ${MIN}포인트 이상이어야 합니다. 입력값: $value"
        }
    }

    companion object {
        const val MIN: Long = 1
    }
}
