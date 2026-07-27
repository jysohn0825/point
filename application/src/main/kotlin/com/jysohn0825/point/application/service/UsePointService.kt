package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.CancelUsagePointDto
import com.jysohn0825.point.application.service.dto.CancelUsagePointResultDto
import com.jysohn0825.point.application.service.dto.PointUsageResultDto
import com.jysohn0825.point.application.service.dto.UsePointDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.event.PointsUsageCancelled
import com.jysohn0825.point.domain.event.PointsUsed
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
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import com.jysohn0825.point.support.key.DistributedKeyGenerator.Companion.EARNING_KEY_NAME
import com.jysohn0825.point.support.lock.DistributedLock
import org.springframework.context.ApplicationEventPublisher
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
    private val keyGenerator: DistributedKeyGenerator,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val redemptionAllocator: PointRedemptionAllocator = PointRedemptionAllocator()
    private val cancellationAllocator: PointCancellationAllocator = PointCancellationAllocator()

    /**
     * 동일 orderNumber 요청이 진짜 동시(in-flight)에 들어오면 락 획득에 실패해 409로 응답한다.
     * 이미 끝난 요청의 재시도(락은 곧바로 잡힘)는 애플리케이션에서 걸러내지 않고 그대로 실행되며,
     * DB 유니크 제약(uk_usage_order) 위반으로 커밋이 실패해 GlobalExceptionHandler가 이를 409로 응답한다.
     */
    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId + ':' + #dto.orderNumber")
    @Transactional
    fun use(dto: UsePointDto): PointUsageResultDto {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val redeemable: List<PointEarning> = earningRepository.findRedeemableByWalletId(wallet.id)

        val lines: List<UsageLine> = redemptionAllocator.allocate(earnings = redeemable, amount = dto.amount)
        val usedEarningIds: Set<String> = lines.map { it.earningId }.toSet()
        val touchedEarnings: List<PointEarning> = redeemable.filter { it.id in usedEarningIds }

        wallet.decrease(PointAmount(lines.sumOf { it.amount }))
        val usage: PointUsage =
            PointUsage.use(
                id = keyGenerator.next(USAGE_KEY_NAME).toString(),
                orderNumber = OrderNumber(dto.orderNumber),
                lines = lines,
            )

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        earningRepository.updateStatusAll(earnings = touchedEarnings, walletId = wallet.id)
        usageRepository.save(usage = usage, walletId = wallet.id)
        eventPublisher.publishEvent(
            PointsUsed(
                walletId = wallet.id,
                amount = lines.sumOf { it.amount }.negate(),
                balanceAfter = wallet.balance.amount,
                usageId = usage.id,
            ),
        )

        return PointUsageResultDto.of(pointUsage = usage)
    }

    @DistributedLock(key = "'point-usage-lock:' + #dto.memberId + ':' + #dto.usageId")
    @Transactional
    fun cancelUsage(dto: CancelUsagePointDto): CancelUsagePointResultDto {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val usage: PointUsage = usageRepository.findById(dto.usageId)
        val policy: PointPolicy = policyRepository.getCurrent()
        val now: LocalDateTime = LocalDateTime.now()
        val cancelAmount: BigDecimal = dto.amount ?: usage.remainingAmount
        val earningIds: List<String> = usage.lines.map { it.earningId }.distinct()

        val earningsById: Map<String, PointEarning> =
            earningRepository
                .findAllByIds(earningIds)
                .associateBy { it.id }

        val allocation: CancellationAllocation =
            cancellationAllocator.allocate(
                usage = usage,
                cancelAmount = cancelAmount,
                earningsById = earningsById,
                policy = policy,
                now = now,
                nextEarningId = { keyGenerator.next(EARNING_KEY_NAME).toString() },
            )

        // 취소로 복원되는 금액은 RESTORED/RE_EARNED 여부와 무관하게 지갑 잔액을 동일하게 증가시킨다.
        wallet.earn(allocation.totalRestoredAmount)
        usage.cancel(allocation.requestedLines)

        val cancellationId: String = keyGenerator.next(USAGE_CANCELLATION_KEY_NAME).toString()

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
            cancellationId = cancellationId,
            reearnedEarningIds = allocation.reearnedEarningIds,
            canceledAt = now,
        )
        eventPublisher.publishEvent(
            PointsUsageCancelled(
                walletId = wallet.id,
                amount = allocation.totalRestoredAmount.value,
                balanceAfter = wallet.balance.amount,
                cancellationId = cancellationId,
            ),
        )

        return CancelUsagePointResultDto(
            usage = usage,
            requestedLines = allocation.requestedLines,
            reEarnings = allocation.reEarnings,
        )
    }

    @Transactional(readOnly = true)
    fun getUsages(memberId: String): List<PointUsageResultDto> {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return PointUsageResultDto.of(usageRepository.findAllByWalletId(wallet.id))
    }

    @Transactional(readOnly = true)
    fun getUsage(usageId: String): PointUsageResultDto = PointUsageResultDto.of(pointUsage = usageRepository.findById(usageId))

    companion object {
        private const val USAGE_KEY_NAME: String = "point-usage"
        private const val USAGE_CANCELLATION_KEY_NAME: String = "point-usage-cancellation"
    }
}
