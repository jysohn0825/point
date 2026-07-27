package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.exception.requireDomain
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
    val maxEarnPerTransaction: MaxEarnPerTransaction = maxEarnPerTransaction

    val maxHoldingAmount: MaxHoldingAmount = maxHoldingAmount

    val defaultExpirationPeriod: ExpirationPeriod = defaultExpirationPeriod

    fun validateEarnAmount(amount: BigDecimal) {
        requireDomain(amount <= maxEarnPerTransaction.value) {
            "1회 적립 한도(${maxEarnPerTransaction.value})를 초과했습니다: $amount"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointPolicy) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
