package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.lock.DistributedLock
import com.jysohn0825.point.application.service.dto.EarnPointDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EarnPointService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val policyRepository: PointPolicyRepository,
    private val usageRepository: PointUsageRepository,
) {
    /**
     * 동일 (memberId, earnType, sourceReferenceId) 조합의 요청이 재시도되어도,
     * 락으로 직렬화한 뒤 기존 적립건을 그대로 반환해 중복 적립을 막는다.
     */
    @DistributedLock(
        key = "'point-earning-lock:' + #dto.memberId + ':' + #dto.earnType + ':' + #dto.sourceReferenceId",
    )
    @Transactional
    fun earn(dto: EarnPointDto): PointEarning {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(dto.memberId)

        return findExistingEarning(wallet = wallet, dto = dto) ?: createEarning(wallet = wallet, dto = dto)
    }

    private fun findExistingEarning(
        wallet: PointWallet,
        dto: EarnPointDto,
    ): PointEarning? =
        earningRepository.findByWalletIdAndEarnTypeAndSourceReferenceId(
            walletId = wallet.id,
            earnType = dto.earnType,
            sourceReferenceId = dto.sourceReferenceId,
        )

    private fun createEarning(
        wallet: PointWallet,
        dto: EarnPointDto,
    ): PointEarning {
        val policy: PointPolicy = policyRepository.getCurrent()
        policy.validateEarnAmount(dto.amount)

        val pointAmount: PointAmount = PointAmount(dto.amount)
        wallet.earn(pointAmount)

        val earning: PointEarning =
            PointEarning.earn(
                amount = pointAmount,
                earnType = dto.earnType,
                sourceReferenceId = dto.sourceReferenceId,
                grantedBy = dto.grantedByAdminId?.let(::GrantedBy),
                period = dto.expirationPeriod ?: policy.defaultExpirationPeriod,
            )

        walletRepository.save(wallet = wallet, memberId = dto.memberId)
        earningRepository.save(earning = earning, walletId = wallet.id, policyId = policy.id)

        return earning
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
    ): PointEarning {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(memberId)
        val earning: PointEarning = earningRepository.findById(earningId)

        earning.cancelEarning()
        wallet.decrease(earning.amount)

        walletRepository.save(wallet = wallet, memberId = memberId)
        earningRepository.updateStatus(earning = earning, walletId = wallet.id)

        return earning
    }

    /**
     * 조회 API용 — 회원 기준 전체 적립건 목록(상태 무관, 최신순).
     */
    @Transactional(readOnly = true)
    fun getEarnings(memberId: String): List<PointEarning> {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return earningRepository.findAllByWalletId(wallet.id)
    }

    /**
     * 조회 API용 — 적립건 상세. cancelEarning()과 동일하게 지갑 소유권 교차검증은 하지 않는다(기존 관례).
     */
    @Transactional(readOnly = true)
    fun getEarning(earningId: String): PointEarning = earningRepository.findById(earningId)

    /**
     * 조회 API용 — 이 적립건이 어느 주문에서 얼마나 사용됐는지 1원 단위로 역추적한다.
     */
    @Transactional(readOnly = true)
    fun getUsageTraces(earningId: String): List<EarningUsageTrace> = usageRepository.findLinesByEarningId(earningId)
}
