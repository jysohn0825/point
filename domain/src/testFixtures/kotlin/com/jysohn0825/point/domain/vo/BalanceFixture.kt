package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun balance(amount: BigDecimal = BigDecimal(10_000)): Balance = Balance(amount)
