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
    name = "point_wallet",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_wallet_member", columnNames = ["member_id"]),
    ],
)
@Comment("포인트 지갑 (회원당 1행)")
class PointWalletEntity(
    @Id
    @Column(name = "id", length = 36)
    @Comment("지갑 ID")
    val id: String,
    @Column(name = "member_id", nullable = false, length = 36)
    @Comment("회원 ID")
    val memberId: String,
    @Column(name = "balance", nullable = false, precision = 19, scale = 0)
    @Comment("총 잔액")
    val balance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "holding_limit_override", precision = 19, scale = 0)
    @Comment("개인 한도 예외. NULL이면 정책값 적용")
    val holdingLimitOverride: BigDecimal? = null,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
