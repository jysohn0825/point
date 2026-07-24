package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun holdingLimit(value: BigDecimal = BigDecimal(1_000_000)): HoldingLimit = HoldingLimit(value)
