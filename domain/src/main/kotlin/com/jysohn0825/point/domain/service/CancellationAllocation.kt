package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.PointAmount

data class CancellationAllocation(
    val requestedLines: List<CancellationLine>,
    val reearnedEarningIds: List<String>,
    val restoredEarnings: List<PointEarning>,
    val reEarnings: List<PointEarning>,
) {
    val totalRestoredAmount: PointAmount
        get() = PointAmount(requestedLines.sumOf { it.restoredAmount })
}
