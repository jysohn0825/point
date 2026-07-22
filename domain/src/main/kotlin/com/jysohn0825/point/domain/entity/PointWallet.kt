package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.domain.vo.PointAmount

class PointWallet private constructor(
    val id: String,
    balance: Balance,
    val holdingLimit: HoldingLimit,
) {
    init {
        require(id.isNotBlank()) { "id는 비어있을 수 없습니다." }
        require(balance.amount <= holdingLimit.value) { "잔액은 보유한도를 초과할 수 없습니다." }
    }

    var balance: Balance = balance
        private set

    fun earn(pointAmount: PointAmount) {
        check(holdingLimit.canAccept(balance, pointAmount)) { "보유한도를 초과하여 적립할 수 없습니다." }
        balance += pointAmount
    }

    fun decrease(pointAmount: PointAmount) {
        balance -= pointAmount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointWallet) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun open(
            id: String,
            holdingLimit: HoldingLimit,
            balance: Balance = Balance.ZERO,
        ): PointWallet = PointWallet(id = id, balance = balance, holdingLimit = holdingLimit)
    }
}
