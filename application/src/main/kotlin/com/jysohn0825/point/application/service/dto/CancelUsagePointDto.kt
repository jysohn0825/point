package com.jysohn0825.point.application.service.dto

import java.math.BigDecimal

data class CancelUsagePointDto(
    val memberId: String,
    val usageId: String,
    /** 클라이언트가 지정하는 취소 요청 멱등키. 같은 값으로 재시도해도 중복 취소되지 않는다. */
    val requestId: String,
    /** 미지정 시 남은 사용 금액 전체를 취소한다. */
    val amount: BigDecimal? = null,
)
