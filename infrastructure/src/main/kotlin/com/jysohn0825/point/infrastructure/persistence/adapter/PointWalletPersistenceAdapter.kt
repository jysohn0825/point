package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import com.jysohn0825.point.infrastructure.persistence.repository.PointWalletJpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class PointWalletPersistenceAdapter(
    private val jpaRepository: PointWalletJpaRepository,
    private val policyRepository: PointPolicyRepository,
) : PointWalletRepository {
    override fun findByMemberIdForUpdate(memberId: String): PointWallet {
        val entity =
            jpaRepository.findByMemberIdForUpdate(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return entity.toDomain(resolveHoldingLimit(entity))
    }

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        // holdingLimitOverride는 도메인 PointWallet이 들고 있지 않은 컬럼이므로(항상 balance만 변경),
        // 기존 row의 값을 그대로 보존해서 다시 쓴다. 신규 지갑이면 기존 row가 없어 null(정책값 적용)로 유지된다.
        val existingOverride = jpaRepository.findById(wallet.id).orElse(null)?.holdingLimitOverride
        jpaRepository.save(wallet.toEntity(memberId, existingOverride))
    }

    private fun resolveHoldingLimit(entity: PointWalletEntity): HoldingLimit =
        entity.holdingLimitOverride
            ?.let { HoldingLimit(BigDecimal(it)) }
            ?: HoldingLimit(policyRepository.getCurrent().maxHoldingAmount.value)
}

private fun PointWalletEntity.toDomain(holdingLimit: HoldingLimit): PointWallet =
    PointWallet.open(
        id = id,
        holdingLimit = holdingLimit,
        balance = Balance(BigDecimal.valueOf(balance)),
    )

private fun PointWallet.toEntity(
    memberId: String,
    holdingLimitOverride: Long?,
): PointWalletEntity =
    PointWalletEntity(
        id = id,
        memberId = memberId,
        balance = balance.amount.longValueExact(),
        holdingLimitOverride = holdingLimitOverride,
    )
