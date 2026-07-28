package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointUsage
import java.math.BigDecimal
import java.time.LocalDateTime

/** 사용취소 요청 금액을 원 적립건에 어떻게 복원(또는 신규 적립으로 대체)할지 결정하는 정책 포트. */
interface PointCancellationAllocator {
    fun allocate(
        usage: PointUsage,
        cancelAmount: BigDecimal,
        earningsById: Map<String, PointEarning>,
        policy: PointPolicy,
        now: LocalDateTime,
        nextEarningId: () -> String,
    ): CancellationAllocation
}
