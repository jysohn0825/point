package com.jysohn0825.point.domain.vo

import java.math.BigDecimal

fun cancellationLine(
    originalLine: UsageLine = usageLine(),
    restoredAmount: BigDecimal = originalLine.amount,
    restorationType: RestorationType = RestorationType.RESTORED,
): CancellationLine =
    CancellationLine(
        originalLine = originalLine,
        restoredAmount = restoredAmount,
        restorationType = restorationType,
    )
