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
        val existing = jpaRepository.findById(earning.id).orElse(null)
        jpaRepository.save(earning.toEntity(walletId, policyId, existing))
    }

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) {
        val existingById = jpaRepository.findAllByIdIn(earnings.map { it.id }).associateBy { it.id }
        jpaRepository.saveAll(earnings.map { it.toEntity(walletId, policyId, existingById[it.id]) })
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) {
        val existing = earning.requireExisting()
        jpaRepository.save(earning.toEntity(walletId, existing.policyId, existing))
    }

    override fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    ) {
        val existingById = jpaRepository.findAllByIdIn(earnings.map { it.id }).associateBy { it.id }
        jpaRepository.saveAll(
            earnings.map { earning ->
                val existing =
                    existingById[earning.id]
                        ?: throw PointDomainException("적립건을 찾을 수 없습니다: earningId=${earning.id}")
                earning.toEntity(walletId, existing.policyId, existing)
            },
        )
    }

    private fun PointEarning.requireExisting(): PointEarningEntity =
        jpaRepository
            .findById(id)
            .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=$id") }

    override fun findById(earningId: String): PointEarning =
        jpaRepository
            .findById(earningId)
            .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=$earningId") }
            .toDomain()

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        jpaRepository.findRedeemableByWalletId(walletId, LocalDateTime.now()).map { it.toDomain() }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = jpaRepository.findAllByIdIn(earningIds).map { it.toDomain() }

    override fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? = jpaRepository.findByWalletIdAndEarnTypeAndSourceReferenceId(walletId, earnType.name, sourceReferenceId)?.toDomain()

    override fun findAllByWalletId(walletId: String): List<PointEarning> =
        jpaRepository.findAllByWalletIdOrderByEarnedAtDesc(walletId).map { it.toDomain() }

    override fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String> = jpaRepository.findExpiredCandidateWalletIds(now)

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> = jpaRepository.findExpiringByWalletId(walletId, now).map { it.toDomain() }
}

private fun PointEarningEntity.toDomain(): PointEarning =
    PointEarning.reconstitute(
        id = id,
        amount = PointAmount(BigDecimal.valueOf(amount)),
        earnType = EarnType.valueOf(earnType),
        sourceReferenceId = sourceReferenceId,
        grantedBy = grantedByAdminId?.let { GrantedBy(it) },
        earnedAt = earnedAt,
        expirationDate = ExpirationDate(expiresAt),
        remainingAmount = RemainingAmount(BigDecimal.valueOf(remainingAmount)),
        status = EarningStatus.valueOf(status),
    )

private fun PointEarning.toEntity(
    walletId: String,
    policyId: String,
    existing: PointEarningEntity?,
): PointEarningEntity =
    PointEarningEntity(
        id = id,
        walletId = walletId,
        policyId = policyId,
        amount = amount.value.longValueExact(),
        remainingAmount = remainingAmount.value.longValueExact(),
        earnType = earnType.name,
        sourceReferenceId = sourceReferenceId,
        grantedByAdminId = grantedBy?.adminId,
        earnedAt = earnedAt,
        expiresAt = expirationDate.value,
        status = status.name,
        // canceledAt은 도메인이 들고 있지 않은 컬럼이라, CANCELED로 새로 전이하는 순간에만 now()로 채우고
        // 그 외에는 기존 값을 보존한다.
        canceledAt =
            when {
                status == EarningStatus.CANCELED && existing?.status != EarningStatus.CANCELED.name -> LocalDateTime.now()
                else -> existing?.canceledAt
            },
    )
