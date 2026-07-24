package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun remainingAmount(value: BigDecimal = BigDecimal(1_000)): RemainingAmount = RemainingAmount(value)
