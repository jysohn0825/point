package com.jysohn0825.point.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

@Schema(description = "관리자 포인트 수기 지급 요청")
data class ManualGrantPointRequest(
    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = false)
    @Schema(description = "지급할 포인트 금액", example = "1000")
    val amount: BigDecimal,
    @field:NotBlank
    @Schema(description = "지급을 처리한 관리자 식별자", example = "admin-01")
    val adminId: String,
    @field:Positive
    @Schema(description = "만료일(일 단위). 미지정 시 정책의 기본 만료일이 적용됨", example = "365", nullable = true)
    val expirationDays: Long? = null,
)
