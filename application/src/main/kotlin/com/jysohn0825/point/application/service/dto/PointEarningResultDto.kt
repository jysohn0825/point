package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.entity.PointEarning

data class PointEarningResultDto(
    val pointEarning: PointEarning,
) {
    companion object {
        fun of(pointEarnings: List<PointEarning>): List<PointEarningResultDto> = pointEarnings.map { pointEarning -> of(pointEarning) }

        fun of(pointEarning: PointEarning): PointEarningResultDto = PointEarningResultDto(pointEarning = pointEarning)
    }
}
