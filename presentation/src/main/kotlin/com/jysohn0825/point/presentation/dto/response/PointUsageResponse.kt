package com.jysohn0825.point.presentation.dto.response

import com.jysohn0825.point.domain.vo.UsageStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "포인트 사용 건")
data class PointUsageResponse(
    @Schema(description = "사용 식별자(pointKey)", example = "C")
    val usageId: String,
    @Schema(description = "회원(지갑) 식별자", example = "member-01")
    val memberId: String,
    @Schema(description = "사용이 발생한 주문번호", example = "A1234")
    val orderNumber: String,
    @Schema(description = "총 사용 금액", example = "1200")
    val totalAmount: BigDecimal,
    @Schema(description = "사용에 사용된 적립 건별 차감 라인")
    val lines: List<UsageLineResponse>,
    @Schema(description = "사용 상태")
    val status: UsageStatus,
)
