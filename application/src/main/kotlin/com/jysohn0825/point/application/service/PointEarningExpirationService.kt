package com.jysohn0825.point.application.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.service.PointExpirationAllocator
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.support.lock.DistributedLock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PointEarningExpirationService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
) {
    private val expirationAllocator: PointExpirationAllocator = PointExpirationAllocator()

    /**
     * 만료 배치의 지갑 단위 처리 단위. memberId를 모르는 상태(walletId만 앎)에서 지갑을 잠근다.
     */
    @DistributedLock(key = "'point-earning-lock:' + #walletId + ':expire'")
    @Transactional
    fun expireWalletEarnings(
        walletId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarning> = expire(walletId = walletId, now = now)

    @Transactional
    fun expireAllDue(now: LocalDateTime = LocalDateTime.now()): Int {
        val walletIds: List<String> = earningRepository.findExpiredCandidateWalletIds(now)
        walletIds.forEach { walletId -> expireWalletEarnings(walletId = walletId, now = now) }
        return walletIds.size
    }

    private fun expire(
        walletId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarning> {
        val wallet: PointWallet = walletRepository.findByIdForUpdate(walletId)
        val dueEarnings: List<PointEarning> = earningRepository.findExpiringByWalletId(walletId = walletId, now = now)
        if (dueEarnings.isEmpty()) return emptyList()

        applyExpiration(wallet = wallet, dueEarnings = dueEarnings)

        walletRepository.updateBalance(wallet)
        earningRepository.updateStatusAll(earnings = dueEarnings, walletId = walletId)
        return dueEarnings
    }

    /**
     * 관리자가 특정 회원의 만료 대상 적립건을 즉시(스케줄러를 기다리지 않고) 처리하도록 트리거한다.
     */
    @Transactional
    fun expireMemberEarningsNow(
        memberId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarning> {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(memberId)
        val dueEarnings: List<PointEarning> = earningRepository.findExpiringByWalletId(walletId = wallet.id, now = now)
        if (dueEarnings.isEmpty()) return emptyList()

        applyExpiration(wallet = wallet, dueEarnings = dueEarnings)

        walletRepository.save(wallet = wallet, memberId = memberId)
        earningRepository.updateStatusAll(earnings = dueEarnings, walletId = wallet.id)
        return dueEarnings
    }

    private fun applyExpiration(
        wallet: PointWallet,
        dueEarnings: List<PointEarning>,
    ) {
        val totalExpired: PointAmount = expirationAllocator.allocate(dueEarnings = dueEarnings)
        wallet.decrease(totalExpired)
    }
}
