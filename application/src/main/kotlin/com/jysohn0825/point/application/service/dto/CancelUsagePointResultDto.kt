package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.CancellationLine

/**
 * usage.cancellationLines는 과거 모든 취소를 합친 flat list라 "이번 요청분"만 뽑아낼 수 없으므로,
 * 컨트롤러 응답에 필요한 이번 요청분(requestedLines)과 신규로 생성된 재적립건(reEarnings)을 별도로 담아 반환한다.
 */
data class CancelUsagePointResultDto(
    val usage: PointUsage,
    val requestedLines: List<CancellationLine>,
    val reEarnings: List<PointEarning>,
)
