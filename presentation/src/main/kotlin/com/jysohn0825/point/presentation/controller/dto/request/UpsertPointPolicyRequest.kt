package com.jysohn0825.point.presentation.controller.dto.request

import com.jysohn0825.point.application.service.dto.UpsertPointPolicyDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "관리자 포인트 정책 생성/변경 요청")
data class UpsertPointPolicyRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "1회 적립 가능한 최대 포인트 (1 ~ 100,000)", example = "100000")
    val maxEarnPerTransaction: BigDecimal,
    @field:NotNull
    @field:Positive
    @Schema(description = "개인별 포인트 보유 한도", example = "1000000")
    val maxHoldingAmount: BigDecimal,
    @field:NotNull
    @field:Positive
    @Schema(description = "기본 만료일(일 단위, 1 ~ 1824)", example = "365")
    val defaultExpirationDays: Long,
    @field:NotBlank
    @Schema(description = "정책을 등록한 관리자 식별자", example = "admin-01")
    val adminId: String,
    @Schema(
        description = "정책 적용 일자. 미지정 시 즉시 적용되고, 미래 날짜면 예약 등록된다.",
        nullable = true,
    )
    val appliedAt: LocalDate? = null,
) {
    fun to(): UpsertPointPolicyDto =
        UpsertPointPolicyDto(
            maxEarnPerTransaction = this.maxEarnPerTransaction,
            maxHoldingAmount = this.maxHoldingAmount,
            defaultExpirationDays = this.defaultExpirationDays,
            appliedAt = this.appliedAt ?: LocalDate.now(),
            createdByAdminId = this.adminId,
        )
}
