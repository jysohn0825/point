package com.jysohn0825.point.presentation.controller.dto.response

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.UsageStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "포인트 사용 건")
data class PointUsageResponse(
    @Schema(description = "사용 식별자(pointKey)", example = "C")
    val usageId: String,
    @Schema(description = "회원 식별자", example = "member-01")
    val memberId: String,
    @Schema(description = "사용이 발생한 주문번호", example = "A1234")
    val orderNumber: String,
    @Schema(description = "총 사용 금액", example = "1200")
    val totalAmount: BigDecimal,
    @Schema(description = "사용에 사용된 적립 건별 차감 라인")
    val lines: List<UsageLineResponse>,
    @Schema(description = "사용 상태")
    val status: UsageStatus,
) {
    @Schema(description = "사용 시 어떤 적립 건에서 얼마를 차감했는지를 나타내는 라인")
    data class UsageLineResponse(
        @Schema(description = "차감된 적립 건 식별자", example = "A")
        val earningId: String,
        @Schema(description = "해당 적립 건에서 차감된 금액", example = "1000")
        val amount: BigDecimal,
    )

    companion object {
        fun of(
            pointUsages: List<PointUsage>,
            memberId: String,
        ): List<PointUsageResponse> = pointUsages.map { pointUsage -> of(pointUsage, memberId) }

        fun of(
            pointUsage: PointUsage,
            memberId: String,
        ): PointUsageResponse =
            PointUsageResponse(
                usageId = pointUsage.id,
                memberId = memberId,
                orderNumber = pointUsage.orderNumber.value,
                totalAmount = pointUsage.totalAmount,
                lines = pointUsage.lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
                status = pointUsage.status,
            )
    }
}
