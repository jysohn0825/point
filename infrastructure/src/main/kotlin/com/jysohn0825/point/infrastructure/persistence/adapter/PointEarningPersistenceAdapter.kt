package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.ExpirationDate
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.RemainingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointEarningJpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class PointEarningPersistenceAdapter(
    private val jpaRepository: PointEarningJpaRepository,
) : PointEarningRepository {
    override fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    ) {
        val existing: PointEarningEntity? = jpaRepository.findById(earning.id).orElse(null)
        jpaRepository.save(toEntity(earning = earning, walletId = walletId, policyId = policyId, existing = existing))
    }

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) {
        val existingById: Map<String, PointEarningEntity> =
            jpaRepository.findAllByIdIn(earnings.map { it.id }).associateBy { it.id }
        jpaRepository.saveAll(
            earnings.map {
                toEntity(earning = it, walletId = walletId, policyId = policyId, existing = existingById[it.id])
            },
        )
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) {
        val existing: PointEarningEntity = requireExisting(earning = earning)
        jpaRepository.save(toEntity(earning = earning, walletId = walletId, policyId = existing.policyId, existing = existing))
    }

    override fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    ) {
        val existingById: Map<String, PointEarningEntity> =
            jpaRepository.findAllByIdIn(earnings.map { it.id }).associateBy { it.id }
        jpaRepository.saveAll(
            earnings.map { earning ->
                val existing: PointEarningEntity =
                    existingById[earning.id]
                        ?: throw PointDomainException("적립건을 찾을 수 없습니다: earningId=${earning.id}")
                toEntity(earning = earning, walletId = walletId, policyId = existing.policyId, existing = existing)
            },
        )
    }

    private fun requireExisting(earning: PointEarning): PointEarningEntity =
        jpaRepository
            .findById(earning.id)
            .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=${earning.id}") }

    override fun findById(earningId: String): PointEarning =
        toDomain(
            jpaRepository
                .findById(earningId)
                .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=$earningId") },
        )

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        jpaRepository.findRedeemableByWalletId(walletId, LocalDateTime.now()).map { toDomain(it) }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = jpaRepository.findAllByIdIn(earningIds).map { toDomain(it) }

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? =
        jpaRepository
            .findByWalletIdAndEarnTypeAndSourceReferenceId(walletId, earnType.name, sourceReferenceId)
            ?.let { toDomain(it) }

    override fun findAllByWalletId(walletId: String): List<PointEarning> =
        jpaRepository.findAllByWalletIdOrderByEarnedAtDesc(walletId).map { toDomain(it) }

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> = jpaRepository.findExpiredCandidateWalletIds(now)

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> = jpaRepository.findExpiringByWalletId(walletId, now).map { toDomain(it) }
}

private fun toDomain(entity: PointEarningEntity): PointEarning =
    PointEarning.reconstitute(
        id = entity.id,
        amount = PointAmount(BigDecimal.valueOf(entity.amount)),
        earnType = EarnType.valueOf(entity.earnType),
        sourceReferenceId = entity.sourceReferenceId,
        grantedBy = entity.grantedByAdminId?.let { GrantedBy(it) },
        earnedAt = entity.earnedAt,
        expirationDate = ExpirationDate(entity.expiresAt),
        remainingAmount = RemainingAmount(BigDecimal.valueOf(entity.remainingAmount)),
        status = EarningStatus.valueOf(entity.status),
    )

private fun toEntity(
    earning: PointEarning,
    walletId: String,
    policyId: String,
    existing: PointEarningEntity?,
): PointEarningEntity =
    PointEarningEntity(
        id = earning.id,
        walletId = walletId,
        policyId = policyId,
        amount = earning.amount.value.longValueExact(),
        remainingAmount = earning.remainingAmount.value.longValueExact(),
        earnType = earning.earnType.name,
        sourceReferenceId = earning.sourceReferenceId,
        grantedByAdminId = earning.grantedBy?.adminId,
        earnedAt = earning.earnedAt,
        expiresAt = earning.expirationDate.value,
        status = earning.status.name,
        // canceledAt은 도메인이 들고 있지 않은 컬럼이라, CANCELED로 새로 전이하는 순간에만 now()로 채우고
        // 그 외에는 기존 값을 보존한다.
        canceledAt =
            when {
                earning.status == EarningStatus.CANCELED && existing?.status != EarningStatus.CANCELED.name -> LocalDateTime.now()
                else -> existing?.canceledAt
            },
    )
