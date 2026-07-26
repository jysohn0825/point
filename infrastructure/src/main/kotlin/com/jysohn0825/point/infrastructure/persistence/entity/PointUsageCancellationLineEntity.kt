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
    name = "point_usage_cancellation_line",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cancellation_line", columnNames = ["cancellation_id", "usage_line_id"]),
    ],
    indexes = [
        Index(name = "idx_cancellation_line_usage_line", columnList = "usage_line_id"),
        Index(name = "idx_cancellation_line_reearned", columnList = "reearned_earning_id"),
    ],
)
@Comment("포인트 사용취소 라인")
class PointUsageCancellationLineEntity(
    @Id
    @Column(name = "id", length = 36)
    @Comment("취소 라인 ID")
    val id: String,
    @Column(name = "cancellation_id", nullable = false, length = 36)
    @Comment("사용취소 ID")
    val cancellationId: String,
    @Column(name = "usage_line_id", nullable = false, length = 36)
    @Comment("복원 대상 원 사용 라인")
    val usageLineId: String,
    @Column(name = "restored_amount", nullable = false, precision = 19, scale = 0)
    @Comment("복원액")
    val restoredAmount: BigDecimal,
    @Column(name = "restore_type", nullable = false, length = 15)
    @Comment("RESTORED / RE_EARNED")
    val restoreType: String,
    @Column(name = "reearned_earning_id", length = 36)
    @Comment("RE_EARNED 시 생성된 신규 적립건")
    val reearnedEarningId: String? = null,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
