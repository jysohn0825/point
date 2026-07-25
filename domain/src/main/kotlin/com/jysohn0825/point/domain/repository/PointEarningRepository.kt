package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointEarning

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

    fun findById(earningId: String): PointEarning

    /** ACTIVE·미만료·잔여금액>0 인 적립건만 조회한다 (사용 시 allocator 입력용). */
    fun findRedeemableByWalletId(walletId: String): List<PointEarning>

    /** 사용취소 시 원 적립건들의 만료 여부를 일괄 판단하기 위해 조회한다. */
    fun findAllByIds(earningIds: List<String>): List<PointEarning>
}
