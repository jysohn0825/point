package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import java.time.LocalDateTime

class FakePointPolicyRepository : PointPolicyRepository {
    var policy: PointPolicy = pointPolicy()
        private set

    var lastSavedAppliedAt: LocalDateTime? = null
        private set

    var lastSavedCreatedByAdminId: String? = null
        private set

    fun reset(policy: PointPolicy = pointPolicy()) {
        this.policy = policy
        lastSavedAppliedAt = null
        lastSavedCreatedByAdminId = null
    }

    override fun getCurrent(): PointPolicy = policy

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) {
        this.policy = policy
        lastSavedAppliedAt = appliedAt
        lastSavedCreatedByAdminId = createdByAdminId
    }
}
