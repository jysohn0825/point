package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.HistoryType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

fun pointWalletHistory(
    id: String = Random.nextLong(0, Long.MAX_VALUE).toString(),
    historyType: HistoryType = HistoryType.EARN,
    amount: BigDecimal = BigDecimal(1_000),
    balanceAfter: BigDecimal = BigDecimal(1_000),
    earningId: String? = Random.nextLong(0, Long.MAX_VALUE).toString(),
    usageId: String? = null,
    cancellationId: String? = null,
    occurredAt: LocalDateTime = LocalDateTime.now(),
): PointWalletHistory =
    PointWalletHistory.record(
        historyType = historyType,
        amount = amount,
        balanceAfter = balanceAfter,
        id = id,
        earningId = earningId,
        usageId = usageId,
        cancellationId = cancellationId,
        occurredAt = occurredAt,
    )
