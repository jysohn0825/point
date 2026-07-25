package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
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
        usageJpaRepository.save(usage.toEntity(walletId))
        lineJpaRepository.saveAll(usage.lines.map { it.toNewEntity(usageId = usage.id) })
    }

    override fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        reearnedEarningIds: List<String?>,
        canceledAt: LocalDateTime,
    ) {
        require(requestedLines.size == reearnedEarningIds.size) {
            "requestedLines와 reearnedEarningIds는 같은 길이여야 합니다."
        }
        requestedLines.forEachIndexed { index, line ->
            val requiresReearnedId = line.restorationType == RestorationType.RE_EARNED
            require((reearnedEarningIds[index] != null) == requiresReearnedId) {
                "reearnedEarningIds[$index]는 RE_EARNED일 때만 값이 있어야 합니다: type=${line.restorationType}"
            }
        }

        val existingLinesByEarningId = lineJpaRepository.findAllByUsageId(usage.id).associateBy { it.earningId }

        // 이번 cancel() 호출 하나 = point_usage_cancellation 헤더 1건
        val cancellationId = UUID.randomUUID().toString()
        cancellationJpaRepository.save(
            PointUsageCancellationEntity(
                id = cancellationId,
                usageId = usage.id,
                restoredAmount = requestedLines.sumOf { it.restoredAmount }.longValueExact(),
                canceledAt = canceledAt,
            ),
        )

        val newCancellationLineEntities =
            requestedLines.mapIndexed { index, line ->
                val usageLineEntity =
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
                    reearnedEarningId = reearnedEarningIds[index],
                )
            }
        cancellationLineJpaRepository.saveAll(newCancellationLineEntities)

        // usage_line.canceled_amount는 해당 라인에 대한 전체 취소 누계이므로, 도메인의 최종 상태로 다시 계산해 반영한다.
        val cancelledAmountByEarningId =
            usage.cancellationLines
                .groupBy { it.originalLine.earningId }
                .mapValues { (_, lines) -> lines.sumOf { it.restoredAmount } }
        val updatedLineEntities =
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

        usageJpaRepository.save(usage.toEntity(walletId))
    }

    override fun findById(usageId: String): PointUsage {
        val entity =
            usageJpaRepository
                .findById(usageId)
                .orElseThrow { PointDomainException("사용건을 찾을 수 없습니다: usageId=$usageId") }
        return entity.toDomain()
    }

    override fun findLinesByEarningId(earningId: String): List<EarningUsageTrace> {
        val lineEntities = lineJpaRepository.findAllByEarningId(earningId)
        val usagesById = usageJpaRepository.findAllById(lineEntities.map { it.usageId }).associateBy { it.id }
        return lineEntities.map { line ->
            EarningUsageTrace(
                orderNumber = OrderNumber(usagesById.getValue(line.usageId).orderNumber),
                amount = BigDecimal.valueOf(line.amount),
            )
        }
    }

    override fun findAllByWalletId(walletId: String): List<PointUsage> =
        usageJpaRepository.findAllByWalletIdOrderByUsedAtDesc(walletId).map { it.toDomain() }

    /** usage 하나를 라인·취소이력까지 포함해 완전히 조립한다 (findById/findAllByWalletId 공용). */
    private fun PointUsageEntity.toDomain(): PointUsage {
        val lineEntities = lineJpaRepository.findAllByUsageId(id)
        val lineEntityById = lineEntities.associateBy { it.id }
        val cancellationIds = cancellationJpaRepository.findAllByUsageId(id).map { it.id }
        val cancellationLineEntities =
            if (cancellationIds.isEmpty()) {
                emptyList()
            } else {
                cancellationLineJpaRepository.findAllByCancellationIdIn(cancellationIds)
            }

        return PointUsage.reconstitute(
            id = id,
            orderNumber = OrderNumber(orderNumber),
            lines = lineEntities.map { it.toUsageLine() },
            usedAt = usedAt,
            cancellationLines =
                cancellationLineEntities.map { cl ->
                    val lineEntity = lineEntityById.getValue(cl.usageLineId)
                    CancellationLine(
                        originalLine = lineEntity.toUsageLine(),
                        restoredAmount = BigDecimal.valueOf(cl.restoredAmount),
                        restorationType = RestorationType.valueOf(cl.restoreType),
                    )
                },
        )
    }
}

private fun PointUsage.toEntity(walletId: String): PointUsageEntity =
    PointUsageEntity(
        id = id,
        walletId = walletId,
        orderNumber = orderNumber.value,
        totalAmount = totalAmount.longValueExact(),
        canceledAmount = cancelledAmount.longValueExact(),
        status = status.name,
        usedAt = usedAt,
    )

private fun UsageLine.toNewEntity(usageId: String): PointUsageLineEntity =
    PointUsageLineEntity(
        id = UUID.randomUUID().toString(),
        usageId = usageId,
        earningId = earningId,
        amount = amount.longValueExact(),
    )

private fun PointUsageLineEntity.toUsageLine(): UsageLine = UsageLine(earningId = earningId, amount = BigDecimal.valueOf(amount))
