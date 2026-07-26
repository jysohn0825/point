package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.entity.PointUsage

data class PointUsageResultDto(
    val pointUsage: PointUsage,
) {
    companion object {
        fun of(pointUsages: List<PointUsage>): List<PointUsageResultDto> = pointUsages.map { pointUsage -> of(pointUsage) }

        fun of(pointUsage: PointUsage): PointUsageResultDto = PointUsageResultDto(pointUsage = pointUsage)
    }
}
