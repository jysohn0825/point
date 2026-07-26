package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWalletHistory
import com.jysohn0825.point.domain.repository.PointWalletHistoryRepository
import com.jysohn0825.point.infrastructure.persistence.adapter.mapper.PointWalletHistoryMapper
import com.jysohn0825.point.infrastructure.persistence.repository.PointWalletHistoryJpaRepository
import org.springframework.stereotype.Repository

@Repository
class PointWalletHistoryPersistenceAdapter(
    private val jpaRepository: PointWalletHistoryJpaRepository,
) : PointWalletHistoryRepository {
    /** 히스토리는 append-only라 기존 row 조회 없이 항상 신규 insert한다. */
    override fun save(
        history: PointWalletHistory,
        walletId: String,
    ) {
        jpaRepository.save(PointWalletHistoryMapper.of(history = history, walletId = walletId))
    }
}
