package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageCancellationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointUsageCancellationJpaRepository : JpaRepository<PointUsageCancellationEntity, String> {
    fun findAllByUsageId(usageId: String): List<PointUsageCancellationEntity>
}
