package com.jysohn0825.point.application.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.service.PointExpirationAllocator
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.support.lock.DistributedLock
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 지갑 하나의 적립 만료 처리를 락+트랜잭션으로 감싸는 단위 서비스.
 * PointEarningExpirationService(배치/관리자 트리거 오케스트레이션)가 이 서비스를 별도 빈으로 주입받아 호출한다.
 * 같은 클래스에 두고 self-invocation으로 호출하면 Spring AOP 프록시(락·트랜잭션 어드바이스)를 건너뛰므로
 * 의도적으로 빈을 분리했다.
 */
@Service
class PointWalletEarningExpirationService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val expirationAllocator: PointExpirationAllocator = PointExpirationAllocator()

    /**
     * 만료 배치/관리자 트리거의 지갑 단위 처리 단위. memberId를 모르는 상태(walletId만 앎)에서 지갑을 잠근다.
     */
    @DistributedLock(key = "'point-earning-lock:' + #walletId + ':expire'")
    @Transactional
    fun expireWalletEarnings(
        walletId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarning> {
        val wallet: PointWallet = walletRepository.findByIdForUpdate(walletId)
        val dueEarnings: List<PointEarning> = earningRepository.findExpiringByWalletId(walletId = walletId, now = now)
        if (dueEarnings.isEmpty()) return emptyList()

        val totalExpired: PointAmount = expirationAllocator.allocate(dueEarnings = dueEarnings)
        wallet.decrease(totalExpired)

        walletRepository.updateBalance(wallet)
        earningRepository.updateStatusAll(earnings = dueEarnings, walletId = walletId)
        publishExpirationEvents(wallet = wallet, dueEarnings = dueEarnings)
        return dueEarnings
    }

    /** 만료는 지갑당 한 번에 여러 적립건이 함께 소멸될 수 있어, 적립건별로 히스토리 이벤트를 하나씩 발행한다. */
    private fun publishExpirationEvents(
        wallet: PointWallet,
        dueEarnings: List<PointEarning>,
    ) {
        dueEarnings.forEach { earning ->
            eventPublisher.publishEvent(
                PointsExpired(
                    walletId = wallet.id,
                    amount = earning.remainingAmount.value.negate(),
                    balanceAfter = wallet.balance.amount,
                    earningId = earning.id,
                ),
            )
        }
    }
}
