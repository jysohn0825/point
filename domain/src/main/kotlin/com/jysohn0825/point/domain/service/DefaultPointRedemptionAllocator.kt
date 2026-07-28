package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.exception.requireDomain
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.UsageLine
import java.math.BigDecimal

/**
 * 사용 가능한 적립건 중 수기지급 우선, 그 다음 만료 임박 순으로 소진하는 기본 정책.
 */
class DefaultPointRedemptionAllocator : PointRedemptionAllocator {
    override fun allocate(
        earnings: List<PointEarning>,
        amount: BigDecimal,
    ): List<UsageLine> {
        var remaining: BigDecimal = amount
        val lines: MutableList<UsageLine> = mutableListOf()
        for (earning in sortedByConsumptionPriority(earnings = earnings)) {
            if (remaining.signum() == 0) break
            val deduction: BigDecimal = minOf(earning.remainingAmount.value, remaining)
            earning.use(deduction)
            lines.add(UsageLine(earningId = earning.id, amount = deduction))
            remaining -= deduction
        }
        requireDomain(remaining.signum() == 0) { "사용 가능한 포인트가 부족합니다: 부족액=$remaining" }
        return lines
    }

    private fun sortedByConsumptionPriority(earnings: List<PointEarning>): List<PointEarning> =
        earnings.sortedWith(
            Comparator<PointEarning> { a, b -> comparePriority(a = a.earnType, b = b.earnType) }
                .thenBy { it.expirationDate.value },
        )

    private fun comparePriority(
        a: EarnType,
        b: EarnType,
    ): Int =
        when {
            a == b -> 0
            a.hasPriorityOver(other = b) -> -1
            else -> 1
        }
}
