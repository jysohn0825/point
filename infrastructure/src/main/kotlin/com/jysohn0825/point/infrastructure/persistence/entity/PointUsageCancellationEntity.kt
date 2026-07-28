package com.jysohn0825.point.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "point_usage_cancellation",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_usage_cancellation_request", columnNames = ["request_id"]),
    ],
    indexes = [
        Index(name = "idx_cancellation_usage", columnList = "usage_id"),
    ],
)
@Comment("포인트 사용취소 헤더")
class PointUsageCancellationEntity(
    @Id
    @Column(name = "id", length = 19)
    @Comment("사용취소 ID")
    val id: String,
    @Column(name = "usage_id", nullable = false, length = 19)
    @Comment("원 사용건")
    val usageId: String,
    @Column(name = "request_id", nullable = false, length = 64)
    @Comment("클라이언트가 지정하는 취소 요청 멱등키")
    val requestId: String,
    @Column(name = "restored_amount", nullable = false, precision = 19, scale = 0)
    @Comment("복원 총액 (라인 합계)")
    val restoredAmount: BigDecimal,
    @Column(name = "canceled_at", nullable = false)
    @Comment("취소 일시")
    val canceledAt: LocalDateTime,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
