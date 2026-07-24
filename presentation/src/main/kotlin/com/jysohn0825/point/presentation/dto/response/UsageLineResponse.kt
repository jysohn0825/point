package com.jysohn0825.point.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "사용 시 어떤 적립 건에서 얼마를 차감했는지를 나타내는 라인")
data class UsageLineResponse(
    @Schema(description = "차감된 적립 건 식별자", example = "A")
    val earningId: String,
    @Schema(description = "해당 적립 건에서 차감된 금액", example = "1000")
    val amount: BigDecimal,
)
