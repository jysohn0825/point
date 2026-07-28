package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointNotFoundException

class FakePointWalletRepository : PointWalletRepository {
    private val walletsByMemberId: MutableMap<String, PointWallet> = mutableMapOf()
    private val memberIdByWalletId: MutableMap<String, String> = mutableMapOf()

    fun clear() {
        walletsByMemberId.clear()
        memberIdByWalletId.clear()
    }

    fun seed(
        memberId: String,
        wallet: PointWallet,
    ) {
        walletsByMemberId[memberId] = wallet
        memberIdByWalletId[wallet.id] = memberId
    }

    override fun findByMemberIdForUpdate(memberId: String): PointWallet =
        walletsByMemberId[memberId] ?: throw PointNotFoundException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")

    override fun findByMemberId(memberId: String): PointWallet? = walletsByMemberId[memberId]

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        walletsByMemberId[memberId] = wallet
        memberIdByWalletId[wallet.id] = memberId
    }
}
