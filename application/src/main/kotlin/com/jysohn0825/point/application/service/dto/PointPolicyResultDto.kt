package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.entity.PointPolicy

data class PointPolicyResultDto(
    val pointPolicy: PointPolicy,
) {
    companion object {
        fun of(pointPolicy: PointPolicy): PointPolicyResultDto = PointPolicyResultDto(pointPolicy = pointPolicy)
    }
}
