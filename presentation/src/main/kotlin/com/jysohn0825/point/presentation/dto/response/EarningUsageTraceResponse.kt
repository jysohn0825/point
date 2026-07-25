package com.jysohn0825.point.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "적립건이 어느 주문에서 얼마나 사용됐는지를 나타내는 추적 라인")
data class EarningUsageTraceResponse(
    @Schema(description = "포인트가 사용된 주문번호", example = "A1234")
    val orderNumber: String,
    @Schema(description = "해당 주문에서 이 적립건으로부터 차감된 금액", example = "1000")
    val amount: BigDecimal,
)
