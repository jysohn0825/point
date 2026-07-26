package com.jysohn0825.point.domain.event

import java.math.BigDecimal

/**
 * 지갑 잔액이 변한 사실을 알리는 도메인 이벤트. 히스토리 기록처럼 정합성 불변식이 없는 부가 처리를
 * 동기 이벤트로 분리하기 위한 것으로, 지갑/원장(적립·사용) 저장 자체는 이 이벤트와 무관하게 서비스가 직접 처리한다.
 */
sealed interface PointWalletHistoryEvent {
    val walletId: String
    val amount: BigDecimal
    val balanceAfter: BigDecimal
}

data class PointsEarned(
    override val walletId: String,
    override val amount: BigDecimal,
    override val balanceAfter: BigDecimal,
    val earningId: String,
) : PointWalletHistoryEvent

data class PointsEarningCancelled(
    override val walletId: String,
    override val amount: BigDecimal,
    override val balanceAfter: BigDecimal,
    val earningId: String,
) : PointWalletHistoryEvent

data class PointsUsed(
    override val walletId: String,
    override val amount: BigDecimal,
    override val balanceAfter: BigDecimal,
    val usageId: String,
) : PointWalletHistoryEvent

data class PointsUsageCancelled(
    override val walletId: String,
    override val amount: BigDecimal,
    override val balanceAfter: BigDecimal,
    val cancellationId: String,
) : PointWalletHistoryEvent

data class PointsExpired(
    override val walletId: String,
    override val amount: BigDecimal,
    override val balanceAfter: BigDecimal,
    val earningId: String,
) : PointWalletHistoryEvent
