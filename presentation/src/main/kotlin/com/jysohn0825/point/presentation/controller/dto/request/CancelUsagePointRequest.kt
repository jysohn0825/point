package com.jysohn0825.point.presentation.controller.dto.request

import com.jysohn0825.point.application.service.dto.CancelUsagePointDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

@Schema(description = "포인트 사용취소 요청")
data class CancelUsagePointRequest(
    @field:NotBlank
    @Schema(
        description = "취소 요청 식별자(멱등키). 같은 값으로 재요청해도 중복 취소되지 않는다",
        example = "CANCEL-REQ-1",
    )
    val requestId: String,
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(
        description = "취소(복원)할 포인트 금액. 미지정 시 남은 사용 금액 전체를 취소함",
        example = "1100",
        nullable = true,
    )
    val amount: BigDecimal? = null,
) {
    fun to(
        memberId: String,
        usageId: String,
    ): CancelUsagePointDto =
        CancelUsagePointDto(
            memberId = memberId,
            usageId = usageId,
            requestId = requestId,
            amount = this.amount,
        )
}
