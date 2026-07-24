package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import java.math.BigDecimal
import java.util.UUID

class PointUsage private constructor(
    val id: String,
    val orderNumber: OrderNumber,
    val lines: List<UsageLine>,
) {
    private val _cancellationLines: MutableList<CancellationLine> = mutableListOf()

    val cancellationLines: List<CancellationLine>
        get() = _cancellationLines.toList()

    val totalAmount: BigDecimal = lines.sumOf { it.amount }

    val cancelledAmount: BigDecimal
        get() = _cancellationLines.sumOf { it.restoredAmount }

    val remainingAmount: BigDecimal
        get() = totalAmount - cancelledAmount

    val status: UsageStatus
        get() =
            when {
                _cancellationLines.isEmpty() -> UsageStatus.USED
                remainingAmount.signum() == 0 -> UsageStatus.FULLY_CANCELED
                else -> UsageStatus.PARTIALLY_CANCELED
            }

    fun cancel(requestedLines: List<CancellationLine>) {
        require(requestedLines.isNotEmpty()) { "취소 라인은 최소 1개 이상이어야 합니다." }
        require(status != UsageStatus.FULLY_CANCELED) { "이미 전액 취소된 사용 건은 취소할 수 없습니다." }

        val cancelledByLine =
            _cancellationLines
                .groupingBy { it.originalLine }
                .fold(BigDecimal.ZERO) { acc, cancellationLine -> acc + cancellationLine.restoredAmount }
                .toMutableMap()

        requestedLines.forEach { requested ->
            require(lines.contains(requested.originalLine)) {
                "취소 대상 라인이 이 사용 건에 속하지 않습니다: ${requested.originalLine}"
            }
            val alreadyCancelled = cancelledByLine.getOrDefault(requested.originalLine, BigDecimal.ZERO)
            val totalCancelled = alreadyCancelled + requested.restoredAmount
            require(totalCancelled <= requested.originalLine.amount) {
                "취소 요청 금액이 해당 라인의 사용 금액을 초과할 수 없습니다: line=${requested.originalLine}, requested=$totalCancelled"
            }
            cancelledByLine[requested.originalLine] = totalCancelled
        }

        _cancellationLines.addAll(requestedLines)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointUsage) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun use(
            orderNumber: OrderNumber,
            lines: List<UsageLine>,
        ): PointUsage {
            require(lines.isNotEmpty()) { "사용 라인은 최소 1개 이상이어야 합니다." }
            return PointUsage(UUID.randomUUID().toString(), orderNumber, lines.toList())
        }
    }
}
