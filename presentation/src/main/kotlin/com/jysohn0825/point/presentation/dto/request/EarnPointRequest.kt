package com.jysohn0825.point.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "포인트 적립 요청")
data class EarnPointRequest(
    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(description = "적립 포인트 금액", example = "1000")
    val amount: BigDecimal,
)
