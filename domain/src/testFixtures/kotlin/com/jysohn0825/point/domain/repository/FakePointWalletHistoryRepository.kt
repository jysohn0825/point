package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWalletHistory

class FakePointWalletHistoryRepository : PointWalletHistoryRepository {
    private val historiesById: MutableMap<String, PointWalletHistory> = mutableMapOf()
    private val walletIdByHistoryId: MutableMap<String, String> = mutableMapOf()

    val savedHistories: List<PointWalletHistory>
        get() = historiesById.values.toList()

    fun clear() {
        historiesById.clear()
        walletIdByHistoryId.clear()
    }

    override fun save(
        history: PointWalletHistory,
        walletId: String,
    ) {
        historiesById[history.id] = history
        walletIdByHistoryId[history.id] = walletId
    }

    fun findAllByWalletId(walletId: String): List<PointWalletHistory> =
        historiesById.values.filter { walletIdByHistoryId[it.id] == walletId }
}
