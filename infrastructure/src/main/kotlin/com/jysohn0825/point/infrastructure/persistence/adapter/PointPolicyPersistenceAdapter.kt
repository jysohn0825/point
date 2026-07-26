package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.infrastructure.persistence.adapter.mapper.PointPolicyMapper
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointPolicyJpaRepository
import com.jysohn0825.point.support.cache.CacheExecutor
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class PointPolicyPersistenceAdapter(
    private val jpaRepository: PointPolicyJpaRepository,
    private val cacheExecutor: CacheExecutor,
) : PointPolicyRepository {
    override fun getCurrent(): PointPolicy =
        cacheExecutor.getOrPut(key = currentCacheKey(), ttl = CACHE_TTL) {
            val entity: PointPolicyEntity =
                jpaRepository.findFirstByAppliedAtLessThanEqualOrderByAppliedAtDesc(LocalDateTime.now())
                    ?: throw PointDomainException("적용 가능한 포인트 정책이 없습니다.")
            PointPolicyMapper.of(entity = entity)
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
            PointPolicyMapper.of(policy = policy, policyVersion = nextVersion, appliedAt = appliedAt, createdByAdminId = createdByAdminId),
        )
        cacheExecutor.evict(currentCacheKey())
    }

    private fun currentCacheKey(): String = "$CACHE_KEY_PREFIX${LocalDate.now()}"

    companion object {
        private const val CACHE_KEY_PREFIX: String = "point-policy:current:"
        private val CACHE_TTL: Duration = Duration.ofDays(1)
    }
}
