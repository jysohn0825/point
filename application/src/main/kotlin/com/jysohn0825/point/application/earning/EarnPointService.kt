package com.jysohn0825.point.application.earning

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.application.lock.DistributedLock
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EarnPointService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val policyRepository: PointPolicyRepository,
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
        val wallet = walletRepository.findByMemberIdForUpdate(dto.memberId)

        return findExistingEarning(wallet, dto) ?: createEarning(wallet, dto)
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
        val policy = policyRepository.getCurrent()
        checkMaxEarnPerTransaction(dto, policy)

        val pointAmount = PointAmount(dto.amount)
        wallet.earn(pointAmount)

        val earning =
            PointEarning.earn(
                amount = pointAmount,
                earnType = dto.earnType,
                sourceReferenceId = dto.sourceReferenceId,
                grantedBy = dto.grantedByAdminId?.let(::GrantedBy),
                period = policy.defaultExpirationPeriod,
            )

        walletRepository.save(wallet, dto.memberId)
        earningRepository.save(earning, wallet.id, policy.id)

        return earning
    }

    private fun checkMaxEarnPerTransaction(
        dto: EarnPointDto,
        policy: PointPolicy,
    ) {
        if (dto.amount > policy.maxEarnPerTransaction.value) {
            throw PointBusinessException(
                "1회 적립 한도(${policy.maxEarnPerTransaction.value})를 초과했습니다: ${dto.amount}",
            )
        }
    }
}
