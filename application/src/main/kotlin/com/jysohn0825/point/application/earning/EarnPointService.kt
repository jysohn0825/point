package com.jysohn0825.point.application.earning

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.lock.DistributedLockExecutor
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class EarnPointService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val policyRepository: PointPolicyRepository,
    private val lockExecutor: DistributedLockExecutor,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    /**
     * 동일 (memberId, earnType, sourceReferenceId) 조합의 요청이 재시도되어도,
     * 락으로 직렬화한 뒤 기존 적립건을 그대로 반환해 중복 적립을 막는다.
     */
    fun earn(command: EarnPointCommand): PointEarning {
        val lockKey = "point-earning-lock:${command.memberId}:${command.earnType}:${command.sourceReferenceId}"

        return lockExecutor.executeWithLock(lockKey) {
            transactionTemplate.execute {
                findExistingEarning(command) ?: createEarning(command)
            } ?: error("적립 트랜잭션 실행 결과가 없습니다.")
        }
    }

    private fun findExistingEarning(command: EarnPointCommand): PointEarning? =
        earningRepository.findByMemberIdAndEarnTypeAndSourceReferenceId(
            memberId = command.memberId,
            earnType = command.earnType,
            sourceReferenceId = command.sourceReferenceId,
        )

    private fun createEarning(command: EarnPointCommand): PointEarning {
        val policy = policyRepository.getCurrent()
        if (command.amount > policy.maxEarnPerTransaction.value) {
            throw PointBusinessException(
                "1회 적립 한도(${policy.maxEarnPerTransaction.value})를 초과했습니다: ${command.amount}",
            )
        }

        val pointAmount = PointAmount(command.amount)
        val wallet = walletRepository.findByMemberIdForUpdate(command.memberId)
        wallet.earn(pointAmount)

        val earning =
            PointEarning.earn(
                amount = pointAmount,
                earnType = command.earnType,
                sourceReferenceId = command.sourceReferenceId,
                grantedBy = command.grantedByAdminId?.let(::GrantedBy),
                period = policy.defaultExpirationPeriod,
            )

        walletRepository.save(wallet)
        earningRepository.save(earning)

        return earning
    }
}
