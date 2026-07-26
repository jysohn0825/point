package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.infrastructure.persistence.adapter.mapper.PointUsageMapper
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageCancellationEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageCancellationLineEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageEntity
import com.jysohn0825.point.infrastructure.persistence.entity.PointUsageLineEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointUsageCancellationJpaRepository
import com.jysohn0825.point.infrastructure.persistence.repository.PointUsageCancellationLineJpaRepository
import com.jysohn0825.point.infrastructure.persistence.repository.PointUsageJpaRepository
import com.jysohn0825.point.infrastructure.persistence.repository.PointUsageLineJpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Repository
class PointUsagePersistenceAdapter(
    private val usageJpaRepository: PointUsageJpaRepository,
    private val lineJpaRepository: PointUsageLineJpaRepository,
    private val cancellationJpaRepository: PointUsageCancellationJpaRepository,
    private val cancellationLineJpaRepository: PointUsageCancellationLineJpaRepository,
) : PointUsageRepository {
    override fun save(
        usage: PointUsage,
        walletId: String,
    ) {
        usageJpaRepository.save(PointUsageMapper.of(usage = usage, walletId = walletId))
        lineJpaRepository.saveAll(usage.lines.map { PointUsageMapper.of(usageLine = it, usageId = usage.id) })
    }

    override fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        reearnedEarningIds: List<String>,
        canceledAt: LocalDateTime,
    ) {
        val reEarnedIndices: List<Int> = requestedLines.indices.filter { requestedLines[it].restorationType == RestorationType.RE_EARNED }
        require(reEarnedIndices.size == reearnedEarningIds.size) {
            "RE_EARNED 취소 라인 수와 reearnedEarningIds 개수가 일치해야 합니다: lines=${reEarnedIndices.size}, ids=${reearnedEarningIds.size}"
        }
        val reearnedEarningIdByIndex: Map<Int, String> = reEarnedIndices.zip(reearnedEarningIds).toMap()

        val existingLinesByEarningId: Map<String, PointUsageLineEntity> =
            lineJpaRepository.findAllByUsageId(usage.id).associateBy { it.earningId }

        val cancellationId: String = UUID.randomUUID().toString()
        cancellationJpaRepository.save(
            PointUsageCancellationEntity(
                id = cancellationId,
                usageId = usage.id,
                restoredAmount = requestedLines.sumOf { it.restoredAmount }.longValueExact(),
                canceledAt = canceledAt,
            ),
        )

        val newCancellationLineEntities: List<PointUsageCancellationLineEntity> =
            requestedLines.mapIndexed { index, line ->
                val usageLineEntity: PointUsageLineEntity =
                    existingLinesByEarningId[line.originalLine.earningId]
                        ?: throw PointDomainException(
                            "취소 대상 사용 라인을 찾을 수 없습니다: usageId=${usage.id}, earningId=${line.originalLine.earningId}",
                        )
                PointUsageCancellationLineEntity(
                    id = UUID.randomUUID().toString(),
                    cancellationId = cancellationId,
                    usageLineId = usageLineEntity.id,
                    restoredAmount = line.restoredAmount.longValueExact(),
                    restoreType = line.restorationType.name,
                    reearnedEarningId = reearnedEarningIdByIndex[index],
                )
            }
        cancellationLineJpaRepository.saveAll(newCancellationLineEntities)

        // usage_line.canceled_amount는 해당 라인에 대한 전체 취소 누계이므로, 도메인의 최종 상태로 다시 계산해 반영한다.
        val cancelledAmountByEarningId: Map<String, BigDecimal> =
            usage.cancellationLines
                .groupBy { it.originalLine.earningId }
                .mapValues { (_, lines) -> lines.sumOf { it.restoredAmount } }
        val updatedLineEntities: List<PointUsageLineEntity> =
            existingLinesByEarningId.values.map { existing ->
                PointUsageLineEntity(
                    id = existing.id,
                    usageId = existing.usageId,
                    earningId = existing.earningId,
                    amount = existing.amount,
                    canceledAmount = cancelledAmountByEarningId[existing.earningId]?.longValueExact() ?: 0L,
                )
            }
        lineJpaRepository.saveAll(updatedLineEntities)

        usageJpaRepository.save(PointUsageMapper.of(usage = usage, walletId = walletId))
    }

    override fun findById(usageId: String): PointUsage {
        val entity: PointUsageEntity =
            usageJpaRepository
                .findById(usageId)
                .orElseThrow { PointDomainException("사용건을 찾을 수 없습니다: usageId=$usageId") }
        return toDomain(entity = entity)
    }

    override fun findLinesByEarningId(earningId: String): List<EarningUsageTrace> {
        val lineEntities: List<PointUsageLineEntity> = lineJpaRepository.findAllByEarningId(earningId)
        val usagesById: Map<String, PointUsageEntity> =
            usageJpaRepository.findAllById(lineEntities.map { it.usageId }).associateBy { it.id }
        return lineEntities.map { line ->
            EarningUsageTrace(
                orderNumber = OrderNumber(usagesById.getValue(line.usageId).orderNumber),
                amount = BigDecimal.valueOf(line.amount),
            )
        }
    }

    override fun findAllByWalletId(walletId: String): List<PointUsage> =
        usageJpaRepository.findAllByWalletIdOrderByUsedAtDesc(walletId).map { toDomain(entity = it) }

    /** usage 하나를 라인·취소이력까지 포함해 완전히 조회한다 (findById/findAllByWalletId 공용). 조립은 매퍼에 위임한다. */
    private fun toDomain(entity: PointUsageEntity): PointUsage {
        val lineEntities: List<PointUsageLineEntity> = lineJpaRepository.findAllByUsageId(entity.id)
        val cancellationIds: List<String> = cancellationJpaRepository.findAllByUsageId(entity.id).map { it.id }
        val cancellationLineEntities: List<PointUsageCancellationLineEntity> =
            if (cancellationIds.isEmpty()) {
                emptyList()
            } else {
                cancellationLineJpaRepository.findAllByCancellationIdIn(cancellationIds)
            }

        return PointUsageMapper.of(
            entity = entity,
            lineEntities = lineEntities,
            cancellationLineEntities = cancellationLineEntities,
        )
    }
}
