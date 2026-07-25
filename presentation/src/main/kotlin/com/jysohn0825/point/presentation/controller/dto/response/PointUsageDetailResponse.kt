package com.jysohn0825.point.presentation.controller.dto.response

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "포인트 사용 건 상세(취소 이력 포함)")
data class PointUsageDetailResponse(
    @Schema(description = "사용 식별자(pointKey)", example = "C")
    val usageId: String,
    @Schema(description = "회원 식별자", example = "member-01")
    val memberId: String,
    @Schema(description = "사용이 발생한 주문번호", example = "A1234")
    val orderNumber: String,
    @Schema(description = "총 사용 금액", example = "1200")
    val totalAmount: BigDecimal,
    @Schema(description = "취소(복원)된 누계 금액", example = "1100")
    val cancelledAmount: BigDecimal,
    @Schema(description = "취소되지 않고 남은 사용 금액", example = "100")
    val remainingAmount: BigDecimal,
    @Schema(description = "사용에 사용된 적립 건별 차감 라인")
    val lines: List<UsageLineResponse>,
    @Schema(description = "이 사용 건에 대한 전체 취소 이력")
    val cancellationLines: List<CancellationLineResponse>,
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

    @Schema(description = "사용취소로 복원된 라인")
    data class CancellationLineResponse(
        @Schema(description = "복원 대상 적립 건 식별자", example = "A")
        val earningId: String,
        @Schema(description = "복원된 금액", example = "1000")
        val restoredAmount: BigDecimal,
        @Schema(
            description = "복원 방식(RESTORED: 기존 적립 건에 복원, RE_EARNED: 만료되어 신규 적립으로 대체)",
        )
        val restorationType: RestorationType,
    )

    companion object {
        fun of(
            pointUsage: PointUsage,
            memberId: String,
        ): PointUsageDetailResponse =
            PointUsageDetailResponse(
                usageId = pointUsage.id,
                memberId = memberId,
                orderNumber = pointUsage.orderNumber.value,
                totalAmount = pointUsage.totalAmount,
                cancelledAmount = pointUsage.cancelledAmount,
                remainingAmount = pointUsage.remainingAmount,
                lines = pointUsage.lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
                cancellationLines =
                    pointUsage.cancellationLines.map {
                        CancellationLineResponse(
                            earningId = it.originalLine.earningId,
                            restoredAmount = it.restoredAmount,
                            restorationType = it.restorationType,
                        )
                    },
                status = pointUsage.status,
            )
    }
}
