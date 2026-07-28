package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun earningUsageTrace(
    orderNumber: OrderNumber = orderNumber(),
    amount: BigDecimal = BigDecimal(1_000),
): EarningUsageTrace = EarningUsageTrace(orderNumber = orderNumber, amount = amount)
