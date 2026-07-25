package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PointPolicyJpaRepository : JpaRepository<PointPolicyEntity, String> {
    fun findFirstByAppliedAtLessThanEqualOrderByAppliedAtDesc(now: LocalDateTime): PointPolicyEntity?

    fun findTopByOrderByPolicyVersionDesc(): PointPolicyEntity?
}
