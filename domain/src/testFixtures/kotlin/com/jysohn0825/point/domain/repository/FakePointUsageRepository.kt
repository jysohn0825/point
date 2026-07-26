package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import java.time.LocalDateTime

class FakePointUsageRepository : PointUsageRepository {
    private val usagesById: MutableMap<String, PointUsage> = mutableMapOf()
    private val walletIdByUsageId: MutableMap<String, String> = mutableMapOf()

    var lastSaveCancellationCall: SaveCancellationCall? = null
        private set

    data class SaveCancellationCall(
        val requestedLines: List<CancellationLine>,
        val cancellationId: String,
        val reearnedEarningIds: List<String>,
    )

    fun clear() {
        usagesById.clear()
        walletIdByUsageId.clear()
        lastSaveCancellationCall = null
    }

    override fun save(
        usage: PointUsage,
        walletId: String,
    ) {
        usagesById[usage.id] = usage
        walletIdByUsageId[usage.id] = walletId
    }

    override fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        cancellationId: String,
        reearnedEarningIds: List<String>,
        canceledAt: LocalDateTime,
    ) {
        usagesById[usage.id] = usage
        walletIdByUsageId[usage.id] = walletId
        lastSaveCancellationCall =
            SaveCancellationCall(requestedLines = requestedLines, cancellationId = cancellationId, reearnedEarningIds = reearnedEarningIds)
    }

    override fun findById(usageId: String): PointUsage =
        usagesById[usageId] ?: throw PointDomainException("사용건을 찾을 수 없습니다: usageId=$usageId")

    override fun findLinesByEarningId(earningId: String): List<EarningUsageTrace> =
        usagesById.values.flatMap { usage ->
            usage.lines.filter { it.earningId == earningId }.map { line ->
                EarningUsageTrace(orderNumber = usage.orderNumber, amount = line.amount)
            }
        }

    override fun findAllByWalletId(walletId: String): List<PointUsage> =
        usagesById.values.filter { walletIdByUsageId[it.id] == walletId }.sortedByDescending { it.usedAt }
}
