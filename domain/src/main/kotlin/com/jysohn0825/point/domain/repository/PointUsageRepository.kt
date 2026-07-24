package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.EarningUsageTrace

interface PointUsageRepository {
    fun save(usage: PointUsage)

    fun findById(usageId: String): PointUsage

    /** 적립건이 어느 주문에서 얼마나 사용됐는지 역추적한다. */
    fun findLinesByEarningId(earningId: String): List<EarningUsageTrace>
}
