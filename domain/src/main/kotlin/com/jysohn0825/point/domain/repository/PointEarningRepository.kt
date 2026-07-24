package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointEarning

interface PointEarningRepository {
    fun save(earning: PointEarning)

    fun saveAll(earnings: List<PointEarning>)

    fun findById(earningId: String): PointEarning

    /** ACTIVE·미만료·잔여금액>0 인 적립건만 조회한다 (사용 시 allocator 입력용). */
    fun findRedeemableByMemberId(memberId: String): List<PointEarning>

    /** 사용취소 시 원 적립건들의 만료 여부를 일괄 판단하기 위해 조회한다. */
    fun findAllByIds(earningIds: List<String>): List<PointEarning>
}
