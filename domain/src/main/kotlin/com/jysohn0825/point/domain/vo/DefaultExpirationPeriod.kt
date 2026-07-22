package com.jysohn0825.point.domain.vo

data class DefaultExpirationPeriod(val days: Int) {

    init {
        require(days in MIN_DAYS until MAX_DAYS) {
            "포인트 만료일은 최소 ${MIN_DAYS}일 이상 ${MAX_DAYS}일 미만이어야 합니다. 입력값: $days"
        }
    }

    companion object {
        const val MIN_DAYS: Int = 1
        const val MAX_DAYS: Int = 365 * 5
        const val DEFAULT_DAYS: Int = 365

        fun default(): DefaultExpirationPeriod = DefaultExpirationPeriod(DEFAULT_DAYS)
    }
}
