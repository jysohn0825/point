package com.jysohn0825.point.presentation.config

import com.jysohn0825.point.application.expiration.PointEarningExpirationBatchService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PointEarningExpirationScheduler(
    private val batchService: PointEarningExpirationBatchService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${point.expiration.cron:0 0 1 * * *}")
    fun expireDueEarnings() {
        val processedWalletCount = batchService.expireAllDue()
        log.info("포인트 만료 배치 완료: 처리된 지갑 수={}", processedWalletCount)
    }
}
