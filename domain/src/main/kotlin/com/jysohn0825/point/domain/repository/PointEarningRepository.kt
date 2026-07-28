package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointEarning
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

    /** ACTIVE·미만료·잔여금액>0 인 적립건만 조회한다 (사용 시 allocator 입력용). */
    fun findRedeemableByWalletId(walletId: String): List<PointEarning>

    /** 사용취소 시 원 적립건들의 만료 여부를 일괄 판단하기 위해 조회한다. */
    fun findAllByIds(earningIds: List<String>): List<PointEarning>

    /** 조회 API용 — 상태와 무관하게 지갑의 모든 적립건을 최신순으로 반환한다. */
    fun findAllByWalletId(walletId: String): List<PointEarning>

    /** 특정 지갑에서 ACTIVE·잔여금액>0·이미 만료된 적립건만 조회한다 (findRedeemableByWalletId의 만료 버전). */
    fun findExpiringByWalletId(
        walletId: String,
        now: LocalDateTime,
    ): List<PointEarning>
}
