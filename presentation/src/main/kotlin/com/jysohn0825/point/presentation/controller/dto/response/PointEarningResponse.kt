package com.jysohn0825.point.presentation.controller.dto.response

import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "포인트 적립 건")
data class PointEarningResponse(
    @Schema(description = "적립 식별자(pointKey)", example = "A")
    val earningId: String,
    @Schema(description = "회원 식별자", example = "member-01")
    val memberId: String,
    @Schema(description = "적립 금액", example = "1000")
    val amount: BigDecimal,
    @Schema(description = "사용 가능한 잔여 금액", example = "1000")
    val remainingAmount: BigDecimal,
    @Schema(description = "적립 유형(MANUAL: 수기지급, SYSTEM: 일반 적립)")
    val earnType: EarnType,
    @Schema(description = "수기 지급한 관리자 식별자. SYSTEM 적립인 경우 null", nullable = true)
    val grantedBy: String?,
    @Schema(description = "적립 일시")
    val earnedAt: LocalDateTime,
    @Schema(description = "만료 일시")
    val expirationDate: LocalDateTime,
    @Schema(description = "적립 상태")
    val status: EarningStatus,
) {
    companion object {
        fun of(
            pointEarningResults: List<PointEarningResultDto>,
            memberId: String,
        ): List<PointEarningResponse> =
            pointEarningResults.map { pointEarningResult -> of(pointEarningResult = pointEarningResult, memberId = memberId) }

        fun of(
            pointEarningResult: PointEarningResultDto,
            memberId: String,
        ): PointEarningResponse {
            val pointEarning: PointEarning = pointEarningResult.pointEarning
            return PointEarningResponse(
                earningId = pointEarning.id,
                memberId = memberId,
                amount = pointEarning.amount.value,
                remainingAmount = pointEarning.remainingAmount.value,
                earnType = pointEarning.earnType,
                grantedBy = pointEarning.grantedBy,
                earnedAt = pointEarning.earnedAt,
                expirationDate = pointEarning.expirationDate.value,
                status = pointEarning.status,
            )
        }
    }
}
