package com.jysohn0825.point.domain.vo

enum class EarnType {
    MANUAL,
    SYSTEM,
    ;

    /** 포인트 사용 시 소진 우선순위: MANUAL(수기지급)이 SYSTEM보다 먼저 사용되어야 한다. */
    fun hasPriorityOver(other: EarnType): Boolean = this.priorityOrder() < other.priorityOrder()

    private fun priorityOrder(): Int =
        when (this) {
            MANUAL -> 0
            SYSTEM -> 1
        }
}
