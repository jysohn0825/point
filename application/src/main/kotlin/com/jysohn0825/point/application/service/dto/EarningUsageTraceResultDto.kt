package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.vo.EarningUsageTrace

data class EarningUsageTraceResultDto(
    val earningUsageTrace: EarningUsageTrace,
) {
    companion object {
        fun of(earningUsageTraces: List<EarningUsageTrace>): List<EarningUsageTraceResultDto> =
            earningUsageTraces.map { earningUsageTrace -> of(earningUsageTrace) }

        fun of(earningUsageTrace: EarningUsageTrace): EarningUsageTraceResultDto =
            EarningUsageTraceResultDto(earningUsageTrace = earningUsageTrace)
    }
}
