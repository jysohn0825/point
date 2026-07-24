package com.jysohn0825.point.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("지갑 ID")
    val id: Long = 0,
    @Column(name = "member_id", nullable = false)
    @Comment("회원 ID")
    val memberId: Long,
    @Column(name = "balance", nullable = false)
    @Comment("총 잔액")
    val balance: Long = 0,
    @Column(name = "holding_limit_override")
    @Comment("개인 한도 예외. NULL이면 정책값 적용")
    val holdingLimitOverride: Long? = null,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @Comment("수정 일시")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
