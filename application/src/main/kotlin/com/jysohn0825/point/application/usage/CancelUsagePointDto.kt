package com.jysohn0825.point.application.usage

import java.math.BigDecimal

data class CancelUsagePointDto(
    val memberId: String,
    val usageId: String,
    /** 미지정 시 남은 사용 금액 전체를 취소한다. */
    val amount: BigDecimal? = null,
)
