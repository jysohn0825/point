package com.jysohn0825.point.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "포인트 지갑")
data class PointWalletResponse(
    @Schema(description = "지갑 식별자")
    val walletId: String,
    @Schema(description = "회원(지갑) 식별자", example = "member-01")
    val memberId: String,
    @Schema(description = "총 잔액", example = "1400")
    val balance: BigDecimal,
    @Schema(description = "보유 한도", example = "1000000")
    val holdingLimit: BigDecimal,
)
