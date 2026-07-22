package com.jysohn0825.point.domain.vo

data class CancellationLine(
    val originalLine: UsageLine,
    val restoredAmount: Long,
    val restorationType: RestorationType,
) {
    init {
        require(restoredAmount > 0) { "복원액은 0보다 커야 합니다: $restoredAmount" }
        require(restoredAmount <= originalLine.amount) {
            "복원액은 원 사용 라인의 차감액을 초과할 수 없습니다: restoredAmount=$restoredAmount, lineAmount=${originalLine.amount}"
        }
    }
}
