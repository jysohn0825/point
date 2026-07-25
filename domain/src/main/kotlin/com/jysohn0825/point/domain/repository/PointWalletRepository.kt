package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWallet

interface PointWalletRepository {
    /** 잔액·한도 검사를 위해 회원 단위로 잠금을 걸고 지갑을 조회한다. */
    fun findByMemberIdForUpdate(memberId: String): PointWallet

    /** 조회 전용 경로. 잠금을 걸지 않는다. */
    fun findByMemberId(memberId: String): PointWallet?

    /** 배치처럼 memberId 없이 walletId만 아는 상태에서 지갑을 잠글 때 사용한다. */
    fun findByIdForUpdate(walletId: String): PointWallet

    /** memberId는 PointWallet이 들고 있지 않은 값이라(회원당 지갑 1개인 현재 스키마의 FK) 저장 시점에 별도로 전달한다. */
    fun save(
        wallet: PointWallet,
        memberId: String,
    )

    /**
     * memberId를 모르는 상태(예: walletId 기준 배치)에서 잔액만 갱신한다.
     * 어댑터가 기존 row에서 memberId/holdingLimitOverride를 그대로 보존한다.
     */
    fun updateBalance(wallet: PointWallet)
}
