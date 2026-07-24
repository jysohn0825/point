package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.ExpirationDate
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.GrantedBy
import com.jysohn0825.point.domain.vo.PointAmount
import com.jysohn0825.point.domain.vo.RemainingAmount
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class PointEarning private constructor(
    val id: String,
    val amount: PointAmount,
    val earnType: EarnType,
    val grantedBy: GrantedBy?,
    val earnedAt: LocalDateTime,
    val expirationDate: ExpirationDate,
    remainingAmount: RemainingAmount,
    status: EarningStatus,
) {
    var remainingAmount: RemainingAmount = remainingAmount
        private set

    var status: EarningStatus = status
        private set

    fun isExpiredAt(moment: LocalDateTime): Boolean = expirationDate.isExpiredAt(moment)

    fun canCancelEarning(): Boolean = status.isActive() && remainingAmount.isFullAmountOf(amount)

    fun cancelEarning() {
        check(canCancelEarning()) { "일부 사용되었거나 취소 가능한 상태가 아니므로 적립을 취소할 수 없습니다: $status" }
        remainingAmount = RemainingAmount(BigDecimal.ZERO)
        status = EarningStatus.CANCELED
    }

    fun use(amount: BigDecimal) {
        check(status.isActive()) { "사용 가능한 상태가 아닙니다: $status" }
        remainingAmount = remainingAmount.decrease(amount)
        if (remainingAmount.isExhausted()) {
            status = EarningStatus.EXHAUSTED
        }
    }

    fun restoreUsage(amount: BigDecimal) {
        check(status == EarningStatus.ACTIVE || status == EarningStatus.EXHAUSTED) {
            "사용취소로 복원 가능한 상태가 아닙니다: $status"
        }
        remainingAmount = remainingAmount.increase(amount, upTo = this.amount)
        status = EarningStatus.ACTIVE
    }

    fun expire() {
        check(status.isActive()) { "만료 처리 가능한 상태가 아닙니다: $status" }
        status = EarningStatus.EXPIRED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointEarning) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun earn(
            amount: PointAmount,
            earnType: EarnType,
            id: String = UUID.randomUUID().toString(),
            grantedBy: GrantedBy? = null,
            earnedAt: LocalDateTime = LocalDateTime.now(),
            period: ExpirationPeriod = ExpirationPeriod.DEFAULT,
        ): PointEarning {
            require(id.isNotBlank()) { "적립 식별자는 비어있을 수 없습니다." }
            require((earnType == EarnType.MANUAL) == (grantedBy != null)) {
                "수기지급(MANUAL)인 경우에만 지급 관리자(GrantedBy)를 가질 수 있습니다."
            }
            return PointEarning(
                id = id,
                amount = amount,
                earnType = earnType,
                grantedBy = grantedBy,
                earnedAt = earnedAt,
                expirationDate = ExpirationDate.from(earnedAt, period),
                remainingAmount = RemainingAmount(amount.value),
                status = EarningStatus.ACTIVE,
            )
        }
    }
}
