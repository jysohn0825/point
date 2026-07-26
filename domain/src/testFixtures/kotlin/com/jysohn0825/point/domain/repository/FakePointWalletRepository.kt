package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException

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
        walletsByMemberId[memberId] ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")

    override fun findByMemberId(memberId: String): PointWallet? = walletsByMemberId[memberId]

    override fun findByIdForUpdate(walletId: String): PointWallet {
        val memberId: String = memberIdByWalletId[walletId] ?: throw PointDomainException("포인트 지갑을 찾을 수 없습니다: walletId=$walletId")
        return walletsByMemberId.getValue(memberId)
    }

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        walletsByMemberId[memberId] = wallet
        memberIdByWalletId[wallet.id] = memberId
    }

    override fun updateBalance(wallet: PointWallet) {
        val memberId: String =
            memberIdByWalletId[wallet.id] ?: throw PointDomainException("포인트 지갑을 찾을 수 없습니다: walletId=${wallet.id}")
        walletsByMemberId[memberId] = wallet
    }
}
