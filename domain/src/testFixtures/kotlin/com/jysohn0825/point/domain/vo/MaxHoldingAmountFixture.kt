package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun maxHoldingAmount(value: BigDecimal = BigDecimal(1_000_000)): MaxHoldingAmount = MaxHoldingAmount(value)
