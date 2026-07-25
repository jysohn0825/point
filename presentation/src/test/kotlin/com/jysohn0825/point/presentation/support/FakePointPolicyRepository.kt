package com.jysohn0825.point.presentation.support

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import java.time.LocalDateTime

class FakePointPolicyRepository : PointPolicyRepository {
    var policy: PointPolicy = pointPolicy()

    fun reset() {
        policy = pointPolicy()
    }

    override fun getCurrent(): PointPolicy = policy

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) {
        this.policy = policy
    }
}
