package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointPolicy

interface PointPolicyRepository {
    /** 1회 상한·보유한도·기본만료일이 담긴 현재 정책을 조회한다. */
    fun getCurrent(): PointPolicy

    fun save(policy: PointPolicy)
}
