package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.DefaultExpirationPeriod
import com.jysohn0825.point.domain.vo.MaxEarnPerTransaction
import com.jysohn0825.point.domain.vo.MaxHoldingAmount

class PointPolicy(
    val id: String,
    maxEarnPerTransaction: MaxEarnPerTransaction,
    maxHoldingAmount: MaxHoldingAmount,
    defaultExpirationPeriod: DefaultExpirationPeriod,
) {
    var maxEarnPerTransaction: MaxEarnPerTransaction = maxEarnPerTransaction
        private set

    var maxHoldingAmount: MaxHoldingAmount = maxHoldingAmount
        private set

    var defaultExpirationPeriod: DefaultExpirationPeriod = defaultExpirationPeriod
        private set

    fun update(
        maxEarnPerTransaction: MaxEarnPerTransaction = this.maxEarnPerTransaction,
        maxHoldingAmount: MaxHoldingAmount = this.maxHoldingAmount,
        defaultExpirationPeriod: DefaultExpirationPeriod = this.defaultExpirationPeriod,
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
