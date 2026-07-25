package com.jysohn0825.point.presentation.controller.dto.request

import com.jysohn0825.point.application.service.dto.EarnPointDto
import com.jysohn0825.point.domain.vo.EarnType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "포인트 적립 요청")
data class EarnPointRequest(
    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(description = "적립 포인트 금액", example = "1000")
    val amount: BigDecimal,
    @field:NotBlank
    @Schema(
        description = "적립 출처 참조값(주문번호 등). 동일 값으로 재요청해도 중복 적립되지 않는 멱등성 키",
        example = "ORDER-20260722-0001",
    )
    val sourceReferenceId: String,
) {
    fun to(memberId: String): EarnPointDto =
        EarnPointDto(
            memberId = memberId,
            amount = this.amount,
            earnType = EarnType.SYSTEM,
            sourceReferenceId = this.sourceReferenceId,
        )
}
