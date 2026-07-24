package com.jysohn0825.point.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

@Schema(description = "포인트 사용취소 요청")
data class CancelUsagePointRequest(
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(
        description = "취소(복원)할 포인트 금액. 미지정 시 남은 사용 금액 전체를 취소함",
        example = "1100",
        nullable = true,
    )
    val amount: BigDecimal? = null,
)
