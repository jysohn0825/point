package com.jysohn0825.point.application.service.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class UpsertPointPolicyDto(
    val maxEarnPerTransaction: BigDecimal,
    val maxHoldingAmount: BigDecimal,
    val defaultExpirationDays: Long,
    val appliedAt: LocalDateTime,
    val createdByAdminId: String,
)
