package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.requireDomain
import java.math.BigDecimal

data class CancellationLine(
    val originalLine: UsageLine,
    val restoredAmount: BigDecimal,
    val restorationType: RestorationType,
) {
    init {
        requireDomain(restoredAmount > BigDecimal.ZERO) { "복원액은 0보다 커야 합니다: $restoredAmount" }
        requireDomain(restoredAmount <= originalLine.amount) {
            "복원액은 원 사용 라인의 차감액을 초과할 수 없습니다: restoredAmount=$restoredAmount, lineAmount=${originalLine.amount}"
        }
    }
}
