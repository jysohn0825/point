package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.exception.PointDomainException
import java.time.LocalDateTime

class FakePointEarningRepository : PointEarningRepository {
    private val earningsById: MutableMap<String, PointEarning> = mutableMapOf()
    private val walletIdByEarningId: MutableMap<String, String> = mutableMapOf()

    var updateStatusAllCallCount: Int = 0
        private set

    fun clear() {
        earningsById.clear()
        walletIdByEarningId.clear()
        updateStatusAllCallCount = 0
    }

    override fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    ) {
        earningsById[earning.id] = earning
        walletIdByEarningId[earning.id] = walletId
    }

    override fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    ) {
        earnings.forEach { save(earning = it, walletId = walletId, policyId = policyId) }
    }

    override fun updateStatus(
        earning: PointEarning,
        walletId: String,
    ) {
        earningsById[earning.id] = earning
    }

    override fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    ) {
        updateStatusAllCallCount++
        earnings.forEach { earningsById[it.id] = it }
    }

    override fun findById(earningId: String): PointEarning =
        earningsById[earningId] ?: throw PointDomainException("적립건을 찾을 수 없습니다: earningId=$earningId")

    override fun findRedeemableByWalletId(walletId: String): List<PointEarning> =
        earningsById.values.filter {
            walletIdByEarningId[it.id] == walletId &&
                it.status.isActive() &&
                it.remainingAmount.value.signum() > 0 &&
                !it.isExpiredAt(LocalDateTime.now())
        }

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earningsById.values.filter { it.id in earningIds }

    override fun findAllByWalletId(walletId: String): List<PointEarning> =
        earningsById.values.filter { walletIdByEarningId[it.id] == walletId }.sortedByDescending { it.earnedAt }

    override fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning> =
        earningsById.values.filter {
            walletIdByEarningId[it.id] == walletId &&
                it.status.isActive() &&
                it.remainingAmount.value.signum() > 0 &&
                it.isExpiredAt(now)
        }
}
