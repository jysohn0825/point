package com.jysohn0825.point.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "point_policy",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_policy_version", columnNames = ["policy_version"]),
        UniqueConstraint(name = "uk_policy_applied_at", columnNames = ["applied_at"]),
    ],
)
@Comment("포인트 정책 (버전별 이력)")
class PointPolicyEntity(
    @Id
    @Column(name = "id", length = 36)
    @Comment("정책 ID")
    val id: String,
    @Column(name = "policy_version", nullable = false)
    @Comment("정책 버전 (증가)")
    val policyVersion: Int,
    @Column(name = "max_earn_per_transaction", nullable = false, precision = 19, scale = 0)
    @Comment("1회 적립 상한 (1~100,000)")
    val maxEarnPerTransaction: BigDecimal,
    @Column(name = "max_holding_amount", nullable = false, precision = 19, scale = 0)
    @Comment("개인 보유한도 기본값")
    val maxHoldingAmount: BigDecimal,
    @Column(name = "default_expiration_days", nullable = false)
    @Comment("기본 유효기간 (일). 1~1824")
    val defaultExpirationDays: Int,
    @Column(name = "applied_at", nullable = false)
    @Comment("적용 시각. 미래값 = 예약 등록")
    val appliedAt: LocalDateTime,
    @Column(name = "created_by_admin_id", nullable = false, length = 36)
    @Comment("등록 관리자")
    val createdByAdminId: String,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
