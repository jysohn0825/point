package com.jysohn0825.point.presentation.controller.dto.response

import com.jysohn0825.point.application.service.dto.CancelUsagePointResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.RestorationType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "포인트 사용취소 결과")
data class PointUsageCancellationResponse(
    @Schema(description = "취소가 발생한 사용 건 식별자", example = "C")
    val usageId: String,
    @Schema(description = "이번 요청으로 취소(복원)된 금액", example = "1100")
    val cancelledAmount: BigDecimal,
    @Schema(description = "취소 후 남은 사용 금액", example = "100")
    val remainingUsageAmount: BigDecimal,
    @Schema(description = "적립 건별 복원 내역")
    val cancellationLines: List<CancellationLineResponse>,
    @Schema(description = "만료된 적립 건을 대신하여 신규로 생성된 적립 건 목록")
    val reEarnings: List<PointReEarningResponse>,
) {
    @Schema(description = "포인트 재적립 건")
    data class PointReEarningResponse(
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
    )

    @Schema(description = "사용취소로 복원된 라인")
    data class CancellationLineResponse(
        @Schema(description = "복원 대상 적립 건 식별자", example = "A")
        val earningId: String,
        @Schema(description = "복원된 금액", example = "1000")
        val restoredAmount: BigDecimal,
        @Schema(
            description = "복원 방식(RESTORED: 기존 적립 건에 복원, RE_EARNED: 만료되어 신규 적립으로 대체)",
        )
        val restorationType: RestorationType,
    )

    companion object {
        fun of(
            cancelUsagePointResult: CancelUsagePointResultDto,
            memberId: String,
        ): PointUsageCancellationResponse =
            PointUsageCancellationResponse(
                usageId = cancelUsagePointResult.usage.id,
                cancelledAmount = cancelUsagePointResult.requestedLines.sumOf { it.restoredAmount },
                remainingUsageAmount = cancelUsagePointResult.usage.remainingAmount,
                cancellationLines =
                    cancelUsagePointResult.requestedLines.map {
                        CancellationLineResponse(
                            earningId = it.originalLine.earningId,
                            restoredAmount = it.restoredAmount,
                            restorationType = it.restorationType,
                        )
                    },
                reEarnings = of(cancelUsagePointResult.reEarnings, memberId),
            )

        private fun of(
            pointEarnings: List<PointEarning>,
            memberId: String,
        ): List<PointReEarningResponse> =
            pointEarnings.map { pointEarning ->
                PointReEarningResponse(
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
