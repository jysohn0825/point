package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.CancellationLine

data class CancellationAllocation(
    val requestedLines: List<CancellationLine>,
    // requestedLines 중 RE_EARNED인 라인들만, 등장 순서대로 그 신규 적립건의 id를 담는다(RESTORED 라인은 포함하지 않는다).
    val reearnedEarningIds: List<String>,
    val restoredEarnings: List<PointEarning>,
    val reEarnings: List<PointEarning>,
)
