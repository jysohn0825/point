package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageLineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointUsageLineJpaRepository : JpaRepository<PointUsageLineEntity, String> {
    fun findAllByUsageId(usageId: String): List<PointUsageLineEntity>

    fun findAllByEarningId(earningId: String): List<PointUsageLineEntity>
}
