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
    name = "point_earning",
    indexes = [
        Index(name = "idx_earning_fifo", columnList = "wallet_id, status, expires_at"),
        Index(name = "idx_earning_policy", columnList = "policy_id"),
        Index(name = "idx_earning_wallet", columnList = "wallet_id, earned_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_earning_source",
            columnNames = ["wallet_id", "earn_type", "source_reference_id"],
        ),
    ],
)
@Comment("포인트 적립건")
class PointEarningEntity(
    @Id
    @Column(name = "id", length = 19)
    @Comment("적립 ID")
    val id: String,
    @Column(name = "wallet_id", nullable = false, length = 19)
    @Comment("지갑 ID")
    val walletId: String,
    @Column(name = "policy_id", nullable = false, length = 19)
    @Comment("적립 당시 적용 정책")
    val policyId: String,
    @Column(name = "amount", nullable = false, precision = 19, scale = 0)
    @Comment("최초 적립액 (불변)")
    val amount: BigDecimal,
    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 0)
    @Comment("잔여액")
    val remainingAmount: BigDecimal,
    @Column(name = "earn_type", nullable = false, length = 10)
    @Comment("SYSTEM / MANUAL")
    val earnType: String,
    @Column(name = "source_reference_id", nullable = false, length = 64)
    @Comment("적립 출처 참조값 (주문번호 등). 멱등성 판별 키")
    val sourceReferenceId: String,
    @Column(name = "granted_by_admin_id", length = 36)
    @Comment("수기지급 관리자. MANUAL일 때만")
    val grantedByAdminId: String? = null,
    @Column(name = "earned_at", nullable = false)
    @Comment("적립 일시")
    val earnedAt: LocalDateTime,
    @Column(name = "expires_at", nullable = false)
    @Comment("만료 일시 (적립 시점 확정)")
    val expiresAt: LocalDateTime,
    @Column(name = "status", nullable = false, length = 15)
    @Comment("ACTIVE / EXHAUSTED / EXPIRED / CANCELED")
    val status: String,
    @Column(name = "canceled_at")
    @Comment("적립취소 일시")
    val canceledAt: LocalDateTime? = null,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
