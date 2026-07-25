package com.jysohn0825.point.application.usage

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
import com.jysohn0825.point.domain.service.CancellationAllocation
import com.jysohn0825.point.domain.service.PointCancellationAllocator
import com.jysohn0825.point.domain.service.PointRedemptionAllocator
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.PointAmount
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
    private val cancellationAllocator: PointCancellationAllocator = PointCancellationAllocator()

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

    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId")
    @Transactional
    fun cancelUsage(dto: CancelUsagePointDto): CancelUsagePointResult {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val usage: PointUsage = usageRepository.findById(dto.usageId)
        val policy: PointPolicy = policyRepository.getCurrent()
        val now: LocalDateTime = LocalDateTime.now()
        val cancelAmount: BigDecimal = dto.amount ?: usage.remainingAmount

        val earningsById: Map<String, PointEarning> =
            earningRepository
                .findAllByIds(usage.lines.map { it.earningId }.distinct())
                .associateBy { it.id }

        val allocation: CancellationAllocation =
            cancellationAllocator.allocate(
                usage = usage,
                cancelAmount = cancelAmount,
                earningsById = earningsById,
                policy = policy,
                now = now,
            )

        // 취소로 복원되는 금액은 RESTORED/RE_EARNED 여부와 무관하게 지갑 잔액을 동일하게 증가시킨다.
        wallet.earn(PointAmount(cancelAmount))
        usage.cancel(allocation.requestedLines)

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        if (allocation.restoredEarnings.isNotEmpty()) {
            earningRepository.updateStatusAll(earnings = allocation.restoredEarnings, walletId = wallet.id)
        }
        if (allocation.reEarnings.isNotEmpty()) {
            earningRepository.saveAll(earnings = allocation.reEarnings, walletId = wallet.id, policyId = policy.id)
        }
        usageRepository.saveCancellation(
            usage = usage,
            walletId = wallet.id,
            requestedLines = allocation.requestedLines,
            reearnedEarningIds = allocation.reearnedEarningIds,
            canceledAt = now,
        )

        return CancelUsagePointResult(usage = usage, requestedLines = allocation.requestedLines, reEarnings = allocation.reEarnings)
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
