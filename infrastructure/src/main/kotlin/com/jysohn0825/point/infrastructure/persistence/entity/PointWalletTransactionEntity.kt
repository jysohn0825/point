package com.jysohn0825.point.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "point_wallet_transaction",
    indexes = [
        Index(name = "idx_transaction_wallet", columnList = "wallet_id, occurred_at, id"),
        Index(name = "idx_transaction_earning", columnList = "earning_id"),
        Index(name = "idx_transaction_usage", columnList = "usage_id"),
        Index(name = "idx_transaction_cancellation", columnList = "cancellation_id"),
    ],
)
@Comment("포인트 지갑 거래 원장")
class PointWalletTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("거래 ID")
    val id: Long = 0,
    @Column(name = "wallet_id", nullable = false)
    @Comment("지갑 ID")
    val walletId: Long,
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Comment("EARN / USE / EARN_CANCEL / USE_CANCEL / EXPIRE")
    val transactionType: String,
    @Column(name = "amount", nullable = false)
    @Comment("증감액. 증가 +, 감소 -")
    val amount: Long,
    @Column(name = "balance_after", nullable = false)
    @Comment("기록 시점 잔액")
    val balanceAfter: Long,
    @Column(name = "earning_id")
    @Comment("EARN / EARN_CANCEL / EXPIRE")
    val earningId: Long? = null,
    @Column(name = "usage_id")
    @Comment("USE")
    val usageId: Long? = null,
    @Column(name = "cancellation_id")
    @Comment("USE_CANCEL")
    val cancellationId: Long? = null,
    @Column(name = "occurred_at", nullable = false)
    @Comment("발생 일시")
    val occurredAt: LocalDateTime,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
