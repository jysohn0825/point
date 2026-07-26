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
    name = "point_usage_line",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_usage_line", columnNames = ["usage_id", "earning_id"]),
    ],
    indexes = [
        Index(name = "idx_usage_line_earning", columnList = "earning_id"),
    ],
)
@Comment("포인트 사용 라인")
class PointUsageLineEntity(
    @Id
    @Column(name = "id", length = 36)
    @Comment("사용 라인 ID")
    val id: String,
    @Column(name = "usage_id", nullable = false, length = 36)
    @Comment("사용 ID")
    val usageId: String,
    @Column(name = "earning_id", nullable = false, length = 36)
    @Comment("차감 대상 적립건")
    val earningId: String,
    @Column(name = "amount", nullable = false, precision = 19, scale = 0)
    @Comment("이 적립건에서 차감한 금액")
    val amount: BigDecimal,
    @Column(name = "canceled_amount", nullable = false, precision = 19, scale = 0)
    @Comment("복원된 금액 누계")
    val canceledAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
