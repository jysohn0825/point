package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.PointPolicyResultDto
import com.jysohn0825.point.application.service.dto.UpsertPointPolicyDto
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PointPolicyService(
    private val policyRepository: PointPolicyRepository,
) {
    /**
     * point_policy는 버전별 이력 테이블이라 항상 새 row로 추가된다.
     * 최초 등록이든 이후 변경이든 같은 경로를 타므로 별도의 생성/수정 메서드로 나누지 않는다.
     */
    @Transactional
    fun createOrUpdate(dto: UpsertPointPolicyDto): PointPolicyResultDto {
        val policy: PointPolicy =
            PointPolicy(
                id = UUID.randomUUID().toString(),
                maxEarnPerTransaction = MaxEarnPerTransaction(dto.maxEarnPerTransaction),
                maxHoldingAmount = MaxHoldingAmount(dto.maxHoldingAmount),
                defaultExpirationPeriod = ExpirationPeriod(dto.defaultExpirationDays),
            )

        policyRepository.save(policy = policy, appliedAt = dto.appliedAt, createdByAdminId = dto.createdByAdminId)

        return PointPolicyResultDto(pointPolicy = policy)
    }
}
