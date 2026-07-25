package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface PointWalletJpaRepository : JpaRepository<PointWalletEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PointWalletEntity w where w.memberId = :memberId")
    fun findByMemberIdForUpdate(memberId: String): PointWalletEntity?
}
