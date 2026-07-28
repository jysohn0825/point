package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.EarnPointDto
import com.jysohn0825.point.application.service.dto.EarningUsageTraceResultDto
import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.event.PointsEarned
import com.jysohn0825.point.domain.event.PointsEarningCancelled
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.support.key.DistributedKeyGenerator
import com.jysohn0825.point.support.key.DistributedKeyGenerator.Companion.EARNING_KEY_NAME
import com.jysohn0825.point.support.lock.DistributedLock
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class EarnPointService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val policyRepository: PointPolicyRepository,
    private val usageRepository: PointUsageRepository,
    private val keyGenerator: DistributedKeyGenerator,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 동일 (memberId, earnType, sourceReferenceId) 조합의 요청이 진짜 동시(in-flight)에 들어오면
     * 락 획득에 실패해 409로 응답한다
     */
    @DistributedLock(
        key = "'point-earning-lock:' + #dto.memberId + ':' + #dto.earnType + ':' + #dto.sourceReferenceId",
    )
    @Transactional
    fun systemEarn(dto: EarnPointDto): PointEarningResultDto = earn(dto)

    /**
     * 관리자 수기 지급은 멱등성을 두지 않는다(같은 관리자가 같은 회원에게 여러 번 지급할 수 있어야 함).
     * sourceReferenceId가 요청마다 달라(ManualGrantPointRequest 참고) DB 유니크 제약과 충돌하지 않으므로
     * 락 없이 매 요청을 그대로 새 적립건으로 만든다.
     */
    @Transactional
    fun manualEarn(dto: EarnPointDto): PointEarningResultDto = earn(dto)

    private fun earn(dto: EarnPointDto): PointEarningResultDto {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)
        val policy: PointPolicy = policyRepository.getCurrent()
        policy.validateEarnAmount(dto.amount)

        val pointAmount: PointAmount = PointAmount(dto.amount)
        wallet.earn(pointAmount)

        val earning: PointEarning =
            PointEarning.earn(
                id = keyGenerator.next(EARNING_KEY_NAME).toString(),
                amount = pointAmount,
                earnType = dto.earnType,
                sourceReferenceId = dto.sourceReferenceId,
                grantedBy = dto.grantedByAdminId,
                period = dto.expirationPeriod ?: policy.defaultExpirationPeriod,
            )

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        earningRepository.save(earning = earning, walletId = wallet.id, policyId = policy.id)
        eventPublisher.publishEvent(
            PointsEarned(walletId = wallet.id, amount = pointAmount.value, balanceAfter = wallet.balance.amount, earningId = earning.id),
        )

        return PointEarningResultDto.of(pointEarning = earning)
    }

    /**
     * 적립건 중 아직 하나도 사용되지 않은 전액을 취소한다. 일부라도 사용됐다면 canCancelEarning()이 false라
     * PointEarning.cancelEarning()에서 예외가 발생한다.
     */
    @DistributedLock(key = "'point-earning-lock:' + #memberId + ':cancel:' + #earningId")
    @Transactional
    fun cancelEarning(
        memberId: String,
        earningId: String,
    ): PointEarningResultDto {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(memberId)
        val earning: PointEarning = earningRepository.findById(earningId)

        earning.cancelEarning()
        wallet.decrease(earning.amount)

        walletRepository.save(wallet = wallet, memberId = memberId)
        earningRepository.updateStatus(earning = earning, walletId = wallet.id)
        eventPublisher.publishEvent(
            PointsEarningCancelled(
                walletId = wallet.id,
                amount = earning.amount.value.negate(),
                balanceAfter = wallet.balance.amount,
                earningId = earning.id,
            ),
        )

        return PointEarningResultDto.of(pointEarning = earning)
    }

    /**
     * 관리자가 특정 적립건을 자연 만료일과 무관하게 즉시 만료 처리한다.
     * 만료 즉시 처리 API(expireMemberEarningsNow)는 "잔여액>0·이미 만료일 경과"인 건만 대상으로
     * 하므로, 이미 전액 사용된 적립건은 걸리지 않는다. 이 API는 그런 경우에도 관리자가 임의로
     * 만료일을 지금으로 앞당겨(잔여액이 있으면 소멸까지) 강제할 수 있게 한다.
     */
    @DistributedLock(key = "'point-earning-lock:' + #memberId + ':expire:' + #earningId")
    @Transactional
    fun forceExpireEarning(
        memberId: String,
        earningId: String,
    ): PointEarningResultDto {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(memberId)
        val earning: PointEarning = earningRepository.findById(earningId)

        val voidedAmount: BigDecimal = earning.expireImmediately()
        if (voidedAmount.signum() > 0) {
            wallet.decrease(PointAmount(voidedAmount))
            walletRepository.save(wallet = wallet, memberId = memberId)
        }
        earningRepository.updateStatus(earning = earning, walletId = wallet.id)
        if (voidedAmount.signum() > 0) {
            eventPublisher.publishEvent(
                PointsExpired(
                    walletId = wallet.id,
                    amount = voidedAmount.negate(),
                    balanceAfter = wallet.balance.amount,
                    earningId = earning.id,
                ),
            )
        }

        return PointEarningResultDto.of(pointEarning = earning)
    }

    @Transactional(readOnly = true)
    fun getEarnings(memberId: String): List<PointEarningResultDto> {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return PointEarningResultDto.of(earningRepository.findAllByWalletId(wallet.id))
    }

    /** cancelEarning()과 동일하게 지갑 소유권 교차검증은 하지 않는다(기존 관례). */
    @Transactional(readOnly = true)
    fun getEarning(earningId: String): PointEarningResultDto =
        PointEarningResultDto.of(pointEarning = earningRepository.findById(earningId))

    @Transactional(readOnly = true)
    fun getUsageTraces(earningId: String): List<EarningUsageTraceResultDto> =
        EarningUsageTraceResultDto.of(usageRepository.findLinesByEarningId(earningId))
}
