package com.jysohn0825.point.application.service.dto

import java.math.BigDecimal
import java.time.LocalDate

data class UpsertPointPolicyDto(
    val maxEarnPerTransaction: BigDecimal,
    val maxHoldingAmount: BigDecimal,
    val defaultExpirationDays: Long,
    val appliedAt: LocalDate,
    val createdByAdminId: String,
)
