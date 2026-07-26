package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWalletHistory

interface PointWalletHistoryRepository {
    /** walletId는 PointWalletHistory가 들고 있지 않은 값(순수 FK)이라 저장 시점에 별도로 전달한다. */
    fun save(
        history: PointWalletHistory,
        walletId: String,
    )
}
