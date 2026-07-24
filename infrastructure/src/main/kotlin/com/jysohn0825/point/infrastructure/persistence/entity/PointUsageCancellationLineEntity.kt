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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("취소 라인 ID")
    val id: Long = 0,
    @Column(name = "cancellation_id", nullable = false)
    @Comment("사용취소 ID (FK: point_usage_cancellation.id, 논리적 참조)")
    val cancellationId: Long,
    @Column(name = "usage_line_id", nullable = false)
    @Comment("복원 대상 원 사용 라인 (FK: point_usage_line.id, 논리적 참조)")
    val usageLineId: Long,
    @Column(name = "restored_amount", nullable = false)
    @Comment("복원액")
    val restoredAmount: Long,
    @Column(name = "restore_type", nullable = false, length = 15)
    @Comment("RESTORED / RE_EARNED")
    val restoreType: String,
    @Column(name = "reearned_earning_id")
    @Comment("RE_EARNED 시 생성된 신규 적립건 (FK: point_earning.id, 논리적 참조)")
    val reearnedEarningId: Long? = null,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
