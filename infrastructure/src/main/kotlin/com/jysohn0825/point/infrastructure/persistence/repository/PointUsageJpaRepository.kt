package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointUsageJpaRepository : JpaRepository<PointUsageEntity, String>
