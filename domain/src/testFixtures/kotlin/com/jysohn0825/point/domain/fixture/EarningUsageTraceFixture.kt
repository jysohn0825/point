package com.jysohn0825.point.domain.fixture

import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.OrderNumber
import java.math.BigDecimal

fun earningUsageTrace(
    orderNumber: OrderNumber = orderNumber(),
    amount: BigDecimal = BigDecimal(1_000),
): EarningUsageTrace = EarningUsageTrace(orderNumber, amount)
