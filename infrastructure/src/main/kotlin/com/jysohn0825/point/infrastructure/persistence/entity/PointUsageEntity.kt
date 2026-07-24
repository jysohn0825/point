package com.jysohn0825.point.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "point_usage",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_usage_order", columnNames = ["order_number"]),
    ],
    indexes = [
        Index(name = "idx_usage_wallet", columnList = "wallet_id, used_at"),
    ],
)
@Comment("포인트 사용건")
class PointUsageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("사용 ID")
    val id: Long = 0,
    @Column(name = "wallet_id", nullable = false)
    @Comment("지갑 ID")
    val walletId: Long,
    @Column(name = "order_number", nullable = false, length = 30)
    @Comment("주문번호")
    val orderNumber: String,
    @Column(name = "total_amount", nullable = false)
    @Comment("사용 총액 (라인 합계)")
    val totalAmount: Long,
    @Column(name = "canceled_amount", nullable = false)
    @Comment("취소 누계")
    val canceledAmount: Long = 0,
    @Column(name = "status", nullable = false, length = 20)
    @Comment("USED / PARTIALLY_CANCELED / FULLY_CANCELED")
    val status: String,
    @Column(name = "used_at", nullable = false)
    @Comment("사용 일시")
    val usedAt: LocalDateTime,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
