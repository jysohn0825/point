package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount
import java.math.BigDecimal

class PointPolicy(
    val id: String,
    maxEarnPerTransaction: MaxEarnPerTransaction,
    maxHoldingAmount: MaxHoldingAmount,
    defaultExpirationPeriod: ExpirationPeriod,
) {
    var maxEarnPerTransaction: MaxEarnPerTransaction = maxEarnPerTransaction
        private set

    var maxHoldingAmount: MaxHoldingAmount = maxHoldingAmount
        private set

    var defaultExpirationPeriod: ExpirationPeriod = defaultExpirationPeriod
        private set

    fun validateEarnAmount(amount: BigDecimal) {
        require(amount <= maxEarnPerTransaction.value) {
            "1회 적립 한도(${maxEarnPerTransaction.value})를 초과했습니다: $amount"
        }
    }

    fun update(
        maxEarnPerTransaction: MaxEarnPerTransaction = this.maxEarnPerTransaction,
        maxHoldingAmount: MaxHoldingAmount = this.maxHoldingAmount,
        defaultExpirationPeriod: ExpirationPeriod = this.defaultExpirationPeriod,
    ) {
        this.maxEarnPerTransaction = maxEarnPerTransaction
        this.maxHoldingAmount = maxHoldingAmount
        this.defaultExpirationPeriod = defaultExpirationPeriod
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointPolicy) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
