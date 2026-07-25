package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.EarnType
import java.time.LocalDateTime

interface PointEarningRepository {
    /** walletId·policyId는 PointEarning이 들고 있지 않은 값(순수 FK)이라 저장 시점에 별도로 전달한다. */
    fun save(
        earning: PointEarning,
        walletId: String,
        policyId: String,
    )

    fun saveAll(
        earnings: List<PointEarning>,
        walletId: String,
        policyId: String,
    )

    /**
     * 이미 존재하는 적립건의 상태/잔여금액만 갱신한다(취소·사용·사용복원 등 id 기반 갱신).
     * 이런 전이는 policyId를 쓰지 않으므로 어댑터가 기존 값을 그대로 보존한다.
     */
    fun updateStatus(
        earning: PointEarning,
        walletId: String,
    )

    fun updateStatusAll(
        earnings: List<PointEarning>,
        walletId: String,
    )

    fun findById(earningId: String): PointEarning

    /** 동일 (지갑, 적립유형, 출처참조값) 조합의 기존 적립건이 있는지 조회한다 (적립 멱등성 체크용). */
    fun findByWalletIdAndEarnTypeAndSourceReferenceId(
        walletId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning?

    /** ACTIVE·미만료·잔여금액>0 인 적립건만 조회한다 (사용 시 allocator 입력용). */
    fun findRedeemableByWalletId(walletId: String): List<PointEarning>

    /** 사용취소 시 원 적립건들의 만료 여부를 일괄 판단하기 위해 조회한다. */
    fun findAllByIds(earningIds: List<String>): List<PointEarning>

    /** 조회 API용 — 상태와 무관하게 지갑의 모든 적립건을 최신순으로 반환한다. */
    fun findAllByWalletId(walletId: String): List<PointEarning>

    /** ACTIVE·잔여금액>0·이미 만료(expiresAt <= now)인 적립건이 하나라도 있는 지갑 id 목록 (만료 배치의 순회 대상). */
    fun findExpiredCandidateWalletIds(now: LocalDateTime): List<String>

    /** 특정 지갑에서 ACTIVE·잔여금액>0·이미 만료된 적립건만 조회한다 (findRedeemableByWalletId의 만료 버전). */
    fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning>
}
