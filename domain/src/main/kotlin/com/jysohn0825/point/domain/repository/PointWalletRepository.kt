package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWallet

interface PointWalletRepository {
    /** 잔액·한도 검사를 위해 회원 단위로 잠금을 걸고 지갑을 조회한다. */
    fun findByMemberIdForUpdate(memberId: String): PointWallet

    fun save(wallet: PointWallet)
}
