package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageCancellationLineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointUsageCancellationLineJpaRepository : JpaRepository<PointUsageCancellationLineEntity, String> {
    fun findAllByCancellationIdIn(cancellationIds: List<String>): List<PointUsageCancellationLineEntity>
}
