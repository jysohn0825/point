package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.event.PointsExpired
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.service.PointExpirationAllocator
import com.jysohn0825.point.domain.vo.PointAmount
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PointEarningExpirationService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val expirationAllocator: PointExpirationAllocator = PointExpirationAllocator()

    /** 관리자가 특정 회원의 만료 대상 적립건을 즉시(스케줄러를 기다리지 않고) 처리하도록 트리거한다. */
    @Transactional
    fun expireMemberEarningsNow(
        memberId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarningResultDto> {
        val wallet: PointWallet = walletRepository.findByMemberIdForUpdate(memberId)
        val dueEarnings: List<PointEarning> = earningRepository.findExpiringByWalletId(walletId = wallet.id, now = now)
        if (dueEarnings.isEmpty()) return emptyList()

        val totalExpired: PointAmount = expirationAllocator.allocate(dueEarnings = dueEarnings)
        wallet.decrease(totalExpired)

        walletRepository.save(wallet = wallet, memberId = memberId)
        earningRepository.updateStatusAll(earnings = dueEarnings, walletId = wallet.id)
        publishExpirationEvents(wallet = wallet, dueEarnings = dueEarnings)
        return PointEarningResultDto.of(dueEarnings)
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
