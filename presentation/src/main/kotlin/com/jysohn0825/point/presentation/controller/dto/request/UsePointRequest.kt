package com.jysohn0825.point.presentation.controller.dto.request

import com.jysohn0825.point.application.service.dto.UsePointDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "포인트 사용 요청")
data class UsePointRequest(
    @field:NotBlank
    @Schema(description = "포인트를 사용하는 주문번호", example = "A1234")
    val orderNumber: String,
    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(description = "사용할 포인트 금액", example = "1200")
    val amount: BigDecimal,
) {
    fun to(memberId: String): UsePointDto =
        UsePointDto(
            memberId = memberId,
            orderNumber = this.orderNumber,
            amount = this.amount,
        )
}
