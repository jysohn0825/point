package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.infrastructure.persistence.adapter.mapper.PointEarningMapper
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointEarningJpaRepository
import org.springframework.data.repository.findByIdOrNull
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
        val existing: PointEarningEntity? = jpaRepository.findByIdOrNull(earning.id)
        jpaRepository.save(PointEarningMapper.of(earning = earning, walletId = walletId, policyId = policyId, existing = existing))
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
                PointEarningMapper.of(earning = it, walletId = walletId, policyId = policyId, existing = existingById[it.id])
            },
        )
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) {
        val existing: PointEarningEntity = requireExisting(earning = earning)
        jpaRepository.save(PointEarningMapper.of(earning = earning, walletId = walletId, policyId = existing.policyId, existing = existing))
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
                PointEarningMapper.of(earning = earning, walletId = walletId, policyId = existing.policyId, existing = existing)
            },
        )
    }

    private fun requireExisting(earning: PointEarning): PointEarningEntity =
        jpaRepository
            .findById(earning.id)
            .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=${earning.id}") }

    override fun findById(earningId: String): PointEarning =
        PointEarningMapper.of(
            jpaRepository
                .findById(earningId)
                .orElseThrow { PointDomainException("적립건을 찾을 수 없습니다: earningId=$earningId") },
        )

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        jpaRepository
            .findRedeemableByWalletId(
                walletId = walletId,
                status = EarningStatus.ACTIVE.name,
                minRemainingAmount = BigDecimal.ZERO,
                now = LocalDateTime.now(),
            ).map { PointEarningMapper.of(it) }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> =
        jpaRepository.findAllByIdIn(earningIds).map {
            PointEarningMapper.of(it)
        }

    override fun findAllByWalletId(walletId: String): List<PointEarning> =
        jpaRepository.findAllByWalletIdOrderByEarnedAtDesc(walletId).map { PointEarningMapper.of(it) }

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> =
        jpaRepository
            .findExpiringByWalletId(
                walletId = walletId,
                status = EarningStatus.ACTIVE.name,
                minRemainingAmount = BigDecimal.ZERO,
                now = now,
            ).map { PointEarningMapper.of(it) }
}
