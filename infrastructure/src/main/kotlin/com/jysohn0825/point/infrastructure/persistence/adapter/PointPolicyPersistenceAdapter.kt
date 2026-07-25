package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointPolicyJpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Repository
class PointPolicyPersistenceAdapter(
    private val jpaRepository: PointPolicyJpaRepository,
) : PointPolicyRepository {
    override fun getCurrent(): PointPolicy {
        val entity: PointPolicyEntity =
            jpaRepository.findFirstByAppliedAtLessThanEqualOrderByAppliedAtDesc(LocalDateTime.now())
                ?: throw PointDomainException("적용 가능한 포인트 정책이 없습니다.")
        return toDomain(entity = entity)
    }

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) {
        // point_policy는 버전별 이력 테이블(policy_version·applied_at 모두 UNIQUE)이라 save()는 항상 새 row를 추가한다.
        // policy.id는 도메인 쪽 식별자일 뿐 row 식별자로 재사용하지 않고, 버전 번호와 row id는 어댑터가 직접 채번한다.
        val nextVersion: Int = (jpaRepository.findTopByOrderByPolicyVersionDesc()?.policyVersion ?: 0) + 1
        jpaRepository.save(
            toEntity(policy = policy, policyVersion = nextVersion, appliedAt = appliedAt, createdByAdminId = createdByAdminId),
        )
    }
}

private fun toDomain(entity: PointPolicyEntity): PointPolicy =
    PointPolicy(
        id = entity.id,
        maxEarnPerTransaction = MaxEarnPerTransaction(BigDecimal.valueOf(entity.maxEarnPerTransaction.toLong())),
        maxHoldingAmount = MaxHoldingAmount(BigDecimal.valueOf(entity.maxHoldingAmount)),
        defaultExpirationPeriod = ExpirationPeriod(entity.defaultExpirationDays.toLong()),
    )

private fun toEntity(
    policy: PointPolicy,
    policyVersion: Int,
    appliedAt: LocalDateTime,
    createdByAdminId: String,
): PointPolicyEntity =
    PointPolicyEntity(
        id = UUID.randomUUID().toString(),
        policyVersion = policyVersion,
        maxEarnPerTransaction = policy.maxEarnPerTransaction.value.intValueExact(),
        maxHoldingAmount = policy.maxHoldingAmount.value.longValueExact(),
        defaultExpirationDays = policy.defaultExpirationPeriod.days.toInt(),
        appliedAt = appliedAt,
        createdByAdminId = createdByAdminId,
    )
