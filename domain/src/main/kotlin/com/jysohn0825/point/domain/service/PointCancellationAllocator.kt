package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import java.math.BigDecimal
import java.time.LocalDateTime

class PointCancellationAllocator {
    /**
     * 취소(복원) 시점에 원 적립건이 이미 만료됐다면 그 만료 적립건을 되살리지 않고,
     * 정책의 기본 만료일을 적용한 신규 적립(RE_EARNED)으로 대체한다.
     */
    fun allocate(
        usage: PointUsage,
        cancelAmount: BigDecimal,
        earningsById: Map<String, PointEarning>,
        policy: PointPolicy,
        now: LocalDateTime,
    ): CancellationAllocation {
        require(cancelAmount.signum() > 0) { "취소할 사용 금액이 없습니다: usageId=${usage.id}" }
        require(cancelAmount <= usage.remainingAmount) {
            "취소 요청 금액이 남은 사용 금액을 초과할 수 없습니다: requested=$cancelAmount, remaining=${usage.remainingAmount}"
        }

        val allocations: List<Pair<UsageLine, BigDecimal>> = allocateByLine(usage = usage, cancelAmount = cancelAmount)

        val requestedLines: MutableList<CancellationLine> = mutableListOf()
        val reearnedEarningIds: MutableList<String?> = mutableListOf()
        val restoredEarnings: MutableList<PointEarning> = mutableListOf()
        val reEarnings: MutableList<PointEarning> = mutableListOf()

        allocations.forEach { (line, restoreAmount) ->
            val earning: PointEarning = earningsById.getValue(line.earningId)
            if (earning.isExpiredAt(now)) {
                val reEarned: PointEarning =
                    PointEarning.earn(
                        amount = PointAmount(restoreAmount),
                        earnType = earning.earnType,
                        sourceReferenceId = "USAGE_CANCEL:${usage.id}:${earning.id}",
                        grantedBy = earning.grantedBy,
                        earnedAt = now,
                        period = policy.defaultExpirationPeriod,
                    )
                reEarnings.add(reEarned)
                requestedLines.add(
                    CancellationLine(originalLine = line, restoredAmount = restoreAmount, restorationType = RestorationType.RE_EARNED),
                )
                reearnedEarningIds.add(reEarned.id)
            } else {
                earning.restoreUsage(restoreAmount)
                restoredEarnings.add(earning)
                requestedLines.add(
                    CancellationLine(originalLine = line, restoredAmount = restoreAmount, restorationType = RestorationType.RESTORED),
                )
                reearnedEarningIds.add(null)
            }
        }

        return CancellationAllocation(
            requestedLines = requestedLines,
            reearnedEarningIds = reearnedEarningIds,
            restoredEarnings = restoredEarnings,
            reEarnings = reEarnings,
        )
    }

    private fun allocateByLine(
        usage: PointUsage,
        cancelAmount: BigDecimal,
    ): List<Pair<UsageLine, BigDecimal>> {
        val alreadyCancelledByLine: Map<UsageLine, BigDecimal> =
            usage.cancellationLines
                .groupingBy { it.originalLine }
                .fold(BigDecimal.ZERO) { acc, cancellationLine -> acc + cancellationLine.restoredAmount }

        var remaining: BigDecimal = cancelAmount
        val allocations: MutableList<Pair<UsageLine, BigDecimal>> = mutableListOf()
        for (line in usage.lines) {
            if (remaining.signum() == 0) break
            val cancellable: BigDecimal = line.amount - alreadyCancelledByLine.getOrDefault(line, BigDecimal.ZERO)
            if (cancellable.signum() <= 0) continue
            val take: BigDecimal = minOf(cancellable, remaining)
            allocations.add(line to take)
            remaining -= take
        }
        check(remaining.signum() == 0) { "취소 가능한 금액이 부족합니다: 부족액=$remaining" }
        return allocations
    }
}
