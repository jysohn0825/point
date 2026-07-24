package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun maxEarnPerTransaction(value: BigDecimal = BigDecimal(50_000)): MaxEarnPerTransaction = MaxEarnPerTransaction(value)
