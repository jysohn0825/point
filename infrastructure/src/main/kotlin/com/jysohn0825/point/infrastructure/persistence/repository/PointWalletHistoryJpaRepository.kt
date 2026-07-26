package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PointWalletHistoryJpaRepository : JpaRepository<PointWalletHistoryEntity, String>
