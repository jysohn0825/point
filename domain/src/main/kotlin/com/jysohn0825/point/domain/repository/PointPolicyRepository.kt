package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointPolicy
import java.time.LocalDateTime

interface PointPolicyRepository {
    /** 1회 상한·보유한도·기본만료일이 담긴 현재 정책을 조회한다. */
    fun getCurrent(): PointPolicy

    /**
     * point_policy는 버전별 이력 테이블이라 매번 새 row로 추가된다.
     * appliedAt(적용 시각. 미래값이면 예약 등록)·createdByAdminId(등록 관리자)는 PointPolicy가 들고 있지 않은 값이라 전달한다.
     */
    fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    )
}
