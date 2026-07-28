package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.orderNumber
import com.jysohn0825.point.domain.vo.usageLine

fun pointUsage(
    orderNumber: OrderNumber = orderNumber(),
    lines: List<UsageLine> = listOf(usageLine()),
    id: String = "usage-1",
): PointUsage = PointUsage.use(id = id, orderNumber = orderNumber, lines = lines)
