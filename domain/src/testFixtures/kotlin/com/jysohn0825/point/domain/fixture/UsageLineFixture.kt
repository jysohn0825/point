package com.jysohn0825.point.domain.fixture

import com.jysohn0825.point.domain.vo.EarningId
import com.jysohn0825.point.domain.vo.UsageLine

fun usageLine(
    earningId: EarningId = earningId(),
    amount: Long = 1_000L,
): UsageLine = UsageLine(earningId, amount)
