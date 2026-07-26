package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PointEarningJpaRepository : JpaRepository<PointEarningEntity, String> {
    /**
     * ACTIVE·미만료·잔여금액>0 인 적립건만 조회한다.
     * 소진 우선순위(수기지급 우선·만료 임박 우선)는 도메인의 allocator가 전담하므로
     * 여기서는 정렬을 강제하지 않는다.
     */
    @Query(
        """
        select e from PointEarningEntity e
        where e.walletId = :walletId
          and e.status = 'ACTIVE'
          and e.remainingAmount > 0
          and e.expiresAt > :now
        """,
    )
    fun findRedeemableByWalletId(
        @Param("walletId") walletId: String,
        @Param("now") now: LocalDateTime,
    ): List<PointEarningEntity>

    fun findAllByIdIn(earningIds: List<String>): List<PointEarningEntity>

    fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: String,
        sourceReferenceId: String,
    ): PointEarningEntity?

    /** 조회 API용 — 상태 무관, 최신순. */
    fun findAllByWalletIdOrderByEarnedAtDesc(walletId: String): List<PointEarningEntity>

    /** 만료 배치의 순회 대상 지갑 id (ACTIVE·잔여금액>0·이미 만료). */
    @Query(
        """
        select distinct e.walletId from PointEarningEntity e
        where e.status = 'ACTIVE'
          and e.remainingAmount > 0
          and e.expiresAt <= :now
        """,
    )
    fun findExpiredCandidateWalletIds(
        @Param("now") now: LocalDateTime,
    ): List<String>

    /** findRedeemableByWalletId의 만료 버전 — 만료 배치가 특정 지갑에서 처리할 대상 건. */
    @Query(
        """
        select e from PointEarningEntity e
        where e.walletId = :walletId
          and e.status = 'ACTIVE'
          and e.remainingAmount > 0
          and e.expiresAt <= :now
        """,
    )
    fun findExpiringByWalletId(
        @Param("walletId") walletId: String,
        @Param("now") now: LocalDateTime,
    ): List<PointEarningEntity>
}
