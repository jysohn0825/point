package com.jysohn0825.point.presentation.controller.dto.response

import com.jysohn0825.point.application.service.dto.PointPolicyResultDto
import com.jysohn0825.point.domain.entity.PointPolicy
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "포인트 정책")
data class PointPolicyResponse(
    @Schema(description = "정책 식별자")
    val policyId: String,
    @Schema(description = "1회 적립 가능한 최대 포인트", example = "100000")
    val maxEarnPerTransaction: BigDecimal,
    @Schema(description = "개인별 포인트 보유 한도", example = "1000000")
    val maxHoldingAmount: BigDecimal,
    @Schema(description = "기본 만료일(일 단위)", example = "365")
    val defaultExpirationDays: Long,
) {
    companion object {
        fun of(pointPolicyResult: PointPolicyResultDto): PointPolicyResponse {
            val pointPolicy: PointPolicy = pointPolicyResult.pointPolicy
            return PointPolicyResponse(
                policyId = pointPolicy.id,
                maxEarnPerTransaction = pointPolicy.maxEarnPerTransaction.value,
                maxHoldingAmount = pointPolicy.maxHoldingAmount.value,
                defaultExpirationDays = pointPolicy.defaultExpirationPeriod.days,
            )
        }
    }
}
