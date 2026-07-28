package com.jysohn0825.point.presentation.controller.dto.request

import com.jysohn0825.point.application.service.dto.EarnPointDto
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.ExpirationPeriod
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
) {
    /**
     * 관리자는 같은 회원에게 여러 번 수기 지급할 수 있어야 하므로 sourceReferenceId를 adminId 단독으로
     * 고정하지 않는다. 요청 시각을 덧붙여 매 요청마다 값이 달라지게 해 uk_earning_source와 충돌하지 않게 한다.
     */
    fun to(memberId: String): EarnPointDto =
        EarnPointDto(
            memberId = memberId,
            amount = this.amount,
            earnType = EarnType.MANUAL,
            sourceReferenceId = "$adminId:${System.currentTimeMillis()}",
            grantedByAdminId = this.adminId,
            expirationPeriod = this.expirationDays?.let(::ExpirationPeriod),
        )
}
