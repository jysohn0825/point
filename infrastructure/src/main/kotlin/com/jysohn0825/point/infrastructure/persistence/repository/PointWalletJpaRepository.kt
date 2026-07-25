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

    /** 조회 전용 경로. 잠금을 걸지 않는다. */
    fun findByMemberId(memberId: String): PointWalletEntity?

    /** 배치가 memberId 없이 walletId(PK)만 아는 상태에서 지갑을 잠글 때 사용한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PointWalletEntity w where w.id = :walletId")
    fun findByIdForUpdate(walletId: String): PointWalletEntity?
}
