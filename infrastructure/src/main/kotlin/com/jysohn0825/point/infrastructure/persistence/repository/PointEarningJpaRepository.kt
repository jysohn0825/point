package com.jysohn0825.point.infrastructure.persistence.repository

import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime

interface PointEarningJpaRepository : JpaRepository<PointEarningEntity, String> {
    /**
     * status·미만료·잔여금액 기준으로 조회한다.
     * 소진 우선순위(수기지급 우선·만료 임박 우선)는 도메인의 allocator가 전담하므로
     * 여기서는 정렬을 강제하지 않는다. status/minRemainingAmount는 도메인 상수를 어댑터가 그대로 전달한다.
     */
    @Query(
        """
        select e from PointEarningEntity e
        where e.walletId = :walletId
          and e.status = :status
          and e.remainingAmount > :minRemainingAmount
          and e.expiresAt > :now
        """,
    )
    fun findRedeemableByWalletId(
        @Param("walletId") walletId: String,
        @Param("status") status: String,
        @Param("minRemainingAmount") minRemainingAmount: BigDecimal,
        @Param("now") now: LocalDateTime,
    ): List<PointEarningEntity>

    fun findAllByIdIn(earningIds: List<String>): List<PointEarningEntity>

    fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: String,
        sourceReferenceId: String,
    ): PointEarningEntity?

    fun findAllByWalletIdOrderByEarnedAtDesc(walletId: String): List<PointEarningEntity>

    /** findRedeemableByWalletId의 만료 버전 — 만료 즉시 처리가 특정 지갑에서 처리할 대상 건. */
    @Query(
        """
        select e from PointEarningEntity e
        where e.walletId = :walletId
          and e.status = :status
          and e.remainingAmount > :minRemainingAmount
          and e.expiresAt <= :now
        """,
    )
    fun findExpiringByWalletId(
        @Param("walletId") walletId: String,
        @Param("status") status: String,
        @Param("minRemainingAmount") minRemainingAmount: BigDecimal,
        @Param("now") now: LocalDateTime,
    ): List<PointEarningEntity>
}
