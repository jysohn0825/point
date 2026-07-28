package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.UsageLine
import java.math.BigDecimal

/** 사용 요청 금액을 가용 적립건들에 어떤 순서로 소진할지 결정하는 정책 포트. */
interface PointRedemptionAllocator {
    fun allocate(
        earnings: List<PointEarning>,
        amount: BigDecimal,
    ): List<UsageLine>
}
