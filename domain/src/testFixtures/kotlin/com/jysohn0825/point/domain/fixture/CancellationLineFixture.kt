package com.jysohn0825.point.domain.fixture

import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine

fun cancellationLine(
    originalLine: UsageLine = usageLine(),
    restoredAmount: Long = originalLine.amount,
    restorationType: RestorationType = RestorationType.RESTORED,
): CancellationLine = CancellationLine(originalLine, restoredAmount, restorationType)
