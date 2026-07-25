package com.jysohn0825.point.presentation.support

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointUsageRepository
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import java.time.LocalDateTime

class FakePointUsageRepository : PointUsageRepository {
    private val usagesById: MutableMap<String, PointUsage> = mutableMapOf()
    private val walletIdByUsageId: MutableMap<String, String> = mutableMapOf()

    fun clear() {
        usagesById.clear()
        walletIdByUsageId.clear()
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
        reearnedEarningIds: List<String>,
        canceledAt: LocalDateTime,
    ) {
        usagesById[usage.id] = usage
        walletIdByUsageId[usage.id] = walletId
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
