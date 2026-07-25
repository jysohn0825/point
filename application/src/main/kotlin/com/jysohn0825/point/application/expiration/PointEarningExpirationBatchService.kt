package com.jysohn0825.point.application.expiration

import com.jysohn0825.point.domain.repository.PointEarningRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 전체 지갑을 순회하는 만료 배치의 진입점. 지갑 단위 처리(PointEarningExpirationService)를
 * 별도 빈으로 분리해 호출하는 이유는, 같은 클래스 안에서 @Transactional 메서드를 호출하면
 * 프록시를 거치지 않아(self-invocation) 트랜잭션이 걸리지 않는 문제를 피하기 위함이다.
 */
@Service
class PointEarningExpirationBatchService(
    private val earningRepository: PointEarningRepository,
    private val expirationService: PointEarningExpirationService,
) {
    fun expireAllDue(now: LocalDateTime = LocalDateTime.now()): Int {
        val walletIds: List<String> = earningRepository.findExpiredCandidateWalletIds(now)
        walletIds.forEach { walletId -> expirationService.expireWalletEarnings(walletId = walletId, now = now) }
        return walletIds.size
    }
}
