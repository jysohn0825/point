package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun pointAmount(value: BigDecimal = BigDecimal(1_000)): PointAmount = PointAmount(value)
