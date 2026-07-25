package com.jysohn0825.point.application.usage

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.application.lock.DistributedLock
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.service.PointRedemptionAllocator
import com.jysohn0825.point.domain.vo.CancellationLine
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
    private val redemptionAllocator: PointRedemptionAllocator = PointRedemptionAllocator()

    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId")
    @Transactional
    fun use(dto: UsePointDto): PointUsage {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val redeemable: List<PointEarning> = earningRepository.findRedeemableByWalletId(wallet.id)

        val lines: List<UsageLine> = redemptionAllocator.allocate(earnings = redeemable, amount = dto.amount)
        val usedEarningIds: Set<String> = lines.map { it.earningId }.toSet()
        val touchedEarnings: List<PointEarning> = redeemable.filter { it.id in usedEarningIds }

        wallet.decrease(PointAmount(dto.amount))
        val usage: PointUsage = PointUsage.use(orderNumber = OrderNumber(dto.orderNumber), lines = lines)

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        earningRepository.updateStatusAll(earnings = touchedEarnings, walletId = wallet.id)
        usageRepository.save(usage = usage, walletId = wallet.id)

        return usage
    }

    /**
     * 취소(복원) 시점에 원 적립건이 이미 만료됐다면 그 만료 적립건을 되살리지 않고,
     * 정책의 기본 만료일을 적용한 신규 적립(RE_EARNED)으로 대체한다.
     */
    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId")
    @Transactional
    fun cancelUsage(dto: CancelUsagePointDto): CancelUsagePointResult {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val usage: PointUsage = usageRepository.findById(dto.usageId)

        val cancelAmount: BigDecimal = dto.amount ?: usage.remainingAmount
        if (cancelAmount.signum() <= 0) {
            throw PointBusinessException("취소할 사용 금액이 없습니다: usageId=${usage.id}")
        }
        if (cancelAmount > usage.remainingAmount) {
            throw PointBusinessException(
                "취소 요청 금액이 남은 사용 금액을 초과할 수 없습니다: requested=$cancelAmount, remaining=${usage.remainingAmount}",
            )
        }

        val allocations: List<Pair<UsageLine, BigDecimal>> = allocateCancellation(usage = usage, cancelAmount = cancelAmount)
        val earningsById: Map<String, PointEarning> =
            earningRepository
                .findAllByIds(allocations.map { it.first.earningId }.distinct())
                .associateBy { it.id }

        val now: LocalDateTime = LocalDateTime.now()
        val policy: PointPolicy = policyRepository.getCurrent()

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

        // 취소로 복원되는 금액은 RESTORED/RE_EARNED 여부와 무관하게 지갑 잔액을 동일하게 증가시킨다.
        wallet.earn(PointAmount(cancelAmount))
        usage.cancel(requestedLines)

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        if (restoredEarnings.isNotEmpty()) {
            earningRepository.updateStatusAll(earnings = restoredEarnings, walletId = wallet.id)
        }
        if (reEarnings.isNotEmpty()) {
            earningRepository.saveAll(earnings = reEarnings, walletId = wallet.id, policyId = policy.id)
        }
        usageRepository.saveCancellation(
            usage = usage,
            walletId = wallet.id,
            requestedLines = requestedLines,
            reearnedEarningIds = reearnedEarningIds,
            canceledAt = now,
        )

        return CancelUsagePointResult(usage = usage, requestedLines = requestedLines, reEarnings = reEarnings)
    }

    private fun allocateCancellation(
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

    /**
     * 조회 API용 — 회원(지갑) 기준 전체 사용건 목록(최신순).
     */
    @Transactional(readOnly = true)
    fun getUsages(memberId: String): List<PointUsage> {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return usageRepository.findAllByWalletId(wallet.id)
    }

    /**
     * 조회 API용 — 사용건 상세(취소 이력 포함).
     */
    @Transactional(readOnly = true)
    fun getUsage(usageId: String): PointUsage = usageRepository.findById(usageId)
}
