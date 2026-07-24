package com.jysohn0825.point.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

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
    val reEarnings: List<PointEarningResponse>,
)
