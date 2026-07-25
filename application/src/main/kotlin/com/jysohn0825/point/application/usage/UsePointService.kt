package com.jysohn0825.point.application.usage

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.application.lock.DistributedLock
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class UsePointService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val usageRepository: PointUsageRepository,
    private val policyRepository: PointPolicyRepository,
) {
    /**
     * 사용 가능한 적립건 중 수기지급 우선, 그 다음 만료 임박 순으로 소진한다.
     */
    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId")
    @Transactional
    fun use(dto: UsePointDto): PointUsage {
        val wallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val redeemable = earningRepository.findRedeemableByWalletId(wallet.id).sortedByConsumptionPriority()

        val lines = allocateUsage(redeemable, dto.amount)
        val usedEarningIds = lines.map { it.earningId }.toSet()
        val touchedEarnings = redeemable.filter { it.id in usedEarningIds }

        wallet.decrease(PointAmount(dto.amount))
        val usage = PointUsage.use(orderNumber = OrderNumber(dto.orderNumber), lines = lines)

        walletRepository.save(wallet, dto.memberId)
        earningRepository.updateStatusAll(touchedEarnings, wallet.id)
        usageRepository.save(usage, wallet.id)

        return usage
    }

    private fun List<PointEarning>.sortedByConsumptionPriority(): List<PointEarning> =
        sortedWith(compareBy({ it.earnType != EarnType.MANUAL }, { it.expirationDate.value }))

    private fun allocateUsage(
        redeemable: List<PointEarning>,
        amount: BigDecimal,
    ): List<UsageLine> {
        var remaining = amount
        val lines = mutableListOf<UsageLine>()
        for (earning in redeemable) {
            if (remaining.signum() == 0) break
            val deduction = minOf(earning.remainingAmount.value, remaining)
            earning.use(deduction)
            lines.add(UsageLine(earningId = earning.id, amount = deduction))
            remaining -= deduction
        }
        if (remaining.signum() > 0) {
            throw PointBusinessException("사용 가능한 포인트가 부족합니다: 부족액=$remaining")
        }
        return lines
    }

    /**
     * 취소(복원) 시점에 원 적립건이 이미 만료됐다면 그 만료 적립건을 되살리지 않고,
     * 정책의 기본 만료일을 적용한 신규 적립(RE_EARNED)으로 대체한다.
     */
    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId")
    @Transactional
    fun cancelUsage(dto: CancelUsagePointDto): CancelUsagePointResult {
        val wallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val usage = usageRepository.findById(dto.usageId)

        val cancelAmount = dto.amount ?: usage.remainingAmount
        if (cancelAmount.signum() <= 0) {
            throw PointBusinessException("취소할 사용 금액이 없습니다: usageId=${usage.id}")
        }
        if (cancelAmount > usage.remainingAmount) {
            throw PointBusinessException(
                "취소 요청 금액이 남은 사용 금액을 초과할 수 없습니다: requested=$cancelAmount, remaining=${usage.remainingAmount}",
            )
        }

        val allocations = allocateCancellation(usage, cancelAmount)
        val earningsById =
            earningRepository
                .findAllByIds(allocations.map { it.first.earningId }.distinct())
                .associateBy { it.id }

        val now = LocalDateTime.now()
        val policy = policyRepository.getCurrent()

        val requestedLines = mutableListOf<CancellationLine>()
        val reearnedEarningIds = mutableListOf<String?>()
        val restoredEarnings = mutableListOf<PointEarning>()
        val reEarnings = mutableListOf<PointEarning>()

        allocations.forEach { (line, restoreAmount) ->
            val earning = earningsById.getValue(line.earningId)
            if (earning.isExpiredAt(now)) {
                val reEarned =
                    PointEarning.earn(
                        amount = PointAmount(restoreAmount),
                        earnType = earning.earnType,
                        sourceReferenceId = "USAGE_CANCEL:${usage.id}:${earning.id}",
                        grantedBy = earning.grantedBy,
                        earnedAt = now,
                        period = policy.defaultExpirationPeriod,
                    )
                reEarnings.add(reEarned)
                requestedLines.add(CancellationLine(line, restoreAmount, RestorationType.RE_EARNED))
                reearnedEarningIds.add(reEarned.id)
            } else {
                earning.restoreUsage(restoreAmount)
                restoredEarnings.add(earning)
                requestedLines.add(CancellationLine(line, restoreAmount, RestorationType.RESTORED))
                reearnedEarningIds.add(null)
            }
        }

        // 취소로 복원되는 금액은 RESTORED/RE_EARNED 여부와 무관하게 지갑 잔액을 동일하게 증가시킨다.
        wallet.earn(PointAmount(cancelAmount))
        usage.cancel(requestedLines)

        walletRepository.save(wallet, dto.memberId)
        if (restoredEarnings.isNotEmpty()) {
            earningRepository.updateStatusAll(restoredEarnings, wallet.id)
        }
        if (reEarnings.isNotEmpty()) {
            earningRepository.saveAll(reEarnings, wallet.id, policy.id)
        }
        usageRepository.saveCancellation(usage, wallet.id, requestedLines, reearnedEarningIds, now)

        return CancelUsagePointResult(usage, requestedLines, reEarnings)
    }

    private fun allocateCancellation(
        usage: PointUsage,
        cancelAmount: BigDecimal,
    ): List<Pair<UsageLine, BigDecimal>> {
        val alreadyCancelledByLine =
            usage.cancellationLines
                .groupingBy { it.originalLine }
                .fold(BigDecimal.ZERO) { acc, cancellationLine -> acc + cancellationLine.restoredAmount }

        var remaining = cancelAmount
        val allocations = mutableListOf<Pair<UsageLine, BigDecimal>>()
        for (line in usage.lines) {
            if (remaining.signum() == 0) break
            val cancellable = line.amount - alreadyCancelledByLine.getOrDefault(line, BigDecimal.ZERO)
            if (cancellable.signum() <= 0) continue
            val take = minOf(cancellable, remaining)
            allocations.add(line to take)
            remaining -= take
        }
        check(remaining.signum() == 0) { "취소 가능한 금액이 부족합니다: 부족액=$remaining" }
        return allocations
    }
}
