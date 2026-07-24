package com.jysohn0825.point.presentation.dto.response

import com.jysohn0825.point.domain.vo.RestorationType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

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
