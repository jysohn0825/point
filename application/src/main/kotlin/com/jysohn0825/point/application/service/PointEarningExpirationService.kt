package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.PointEarningResultDto
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PointEarningExpirationService(
    private val walletRepository: PointWalletRepository,
    private val earningRepository: PointEarningRepository,
    private val walletEarningExpirationService: PointWalletEarningExpirationService,
) {
    /**
     * 지갑마다 별도 트랜잭션·별도 락으로 처리되도록 이 메서드 자체는 @Transactional을 걸지 않고,
     * 지갑 단위 처리를 별도 빈(PointWalletEarningExpirationService)에 위임한다. 같은 클래스 안에서
     * self-invocation으로 호출하면 Spring AOP 프록시(락 포함)를 우회하기 때문에 의도적으로 빈을 분리했다.
     */
    fun expireAllDue(now: LocalDateTime = LocalDateTime.now()): Int {
        val walletIds: List<String> = earningRepository.findExpiredCandidateWalletIds(now)
        walletIds.forEach { walletId -> walletEarningExpirationService.expireWalletEarnings(walletId = walletId, now = now) }
        return walletIds.size
    }

    /**
     * 관리자가 특정 회원의 만료 대상 적립건을 즉시(스케줄러를 기다리지 않고) 처리하도록 트리거한다.
     * walletId 조회 후 동일한 지갑 단위 처리(락+트랜잭션)에 위임해 배치와 처리 경로를 통일한다.
     */
    fun expireMemberEarningsNow(
        memberId: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): List<PointEarningResultDto> {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        val dueEarnings: List<PointEarning> = walletEarningExpirationService.expireWalletEarnings(walletId = wallet.id, now = now)
        return PointEarningResultDto.of(dueEarnings)
    }
}
