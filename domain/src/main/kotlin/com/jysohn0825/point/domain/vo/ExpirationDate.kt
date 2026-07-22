package com.jysohn0825.point.domain.vo

import java.time.LocalDateTime

@JvmInline
value class ExpirationDate(
    val value: LocalDateTime,
) {
    fun isExpiredAt(moment: LocalDateTime): Boolean = !moment.isBefore(value)

    companion object {
        fun from(
            earnedAt: LocalDateTime,
            period: ExpirationPeriod,
        ): ExpirationDate = ExpirationDate(earnedAt.plusDays(period.days))
    }
}
