package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.PointAmount
import java.math.BigDecimal

/**
 * expire()는 상태만 EXPIRED로 바꾸고 remainingAmount는 그대로 두므로(취소의 CANCELED와 달리
 * "얼마가 만료됐는지" 기록으로 남긴다), 지갑 잔액에서 뺄 금액은 expire() 호출 전에 미리 합산해야 한다.
 */
class DefaultPointExpirationAllocator : PointExpirationAllocator {
    override fun allocate(dueEarnings: List<PointEarning>): PointAmount {
        val totalExpired: BigDecimal = dueEarnings.sumOf { it.remainingAmount.value }
        dueEarnings.forEach { it.expire() }
        return PointAmount(totalExpired)
    }
}
