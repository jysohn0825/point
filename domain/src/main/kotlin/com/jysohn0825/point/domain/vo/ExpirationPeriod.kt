package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain

@JvmInline
value class ExpirationPeriod(
    val days: Long,
) {
    init {
        requireDomain(days in MIN_DAYS..MAX_DAYS) {
            "만료 기간은 ${MIN_DAYS}일 이상 ${MAX_DAYS}일 이하(5년 미만)만 가능합니다: $days"
        }
    }

    companion object {
        const val MIN_DAYS = 1L
        const val MAX_DAYS = 365L * 5 - 1
        const val DEFAULT_DAYS = 365L

        val DEFAULT: ExpirationPeriod = ExpirationPeriod(DEFAULT_DAYS)
    }
}
