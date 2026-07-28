package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun usageLine(
    earningId: String = "1",
    amount: BigDecimal = BigDecimal(1_000),
): UsageLine = UsageLine(earningId = earningId, amount = amount)
