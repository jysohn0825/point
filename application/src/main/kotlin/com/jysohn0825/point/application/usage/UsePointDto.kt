package com.jysohn0825.point.application.usage

import java.math.BigDecimal

data class UsePointDto(
    val memberId: String,
    val orderNumber: String,
    val amount: BigDecimal,
)
