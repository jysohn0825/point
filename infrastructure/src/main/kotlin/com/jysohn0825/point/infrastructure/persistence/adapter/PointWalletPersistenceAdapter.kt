package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.infrastructure.persistence.adapter.mapper.PointWalletMapper
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
        val entity: PointWalletEntity =
            jpaRepository.findByMemberIdForUpdate(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return PointWalletMapper.of(entity = entity, holdingLimit = resolveHoldingLimit(entity = entity))
    }

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        // holdingLimitOverride는 도메인 PointWallet이 들고 있지 않은 컬럼이므로(항상 balance만 변경),
        // 기존 row의 값을 그대로 보존해서 다시 쓴다. 신규 지갑이면 기존 row가 없어 null(정책값 적용)로 유지된다.
        val existingOverride: BigDecimal? = jpaRepository.findById(wallet.id).orElse(null)?.holdingLimitOverride
        jpaRepository.save(PointWalletMapper.of(wallet = wallet, memberId = memberId, holdingLimitOverride = existingOverride))
    }

    override fun findByMemberId(memberId: String): PointWallet? =
        jpaRepository.findByMemberId(memberId)?.let { PointWalletMapper.of(entity = it, holdingLimit = resolveHoldingLimit(entity = it)) }

    override fun findByIdForUpdate(walletId: String): PointWallet {
        val entity: PointWalletEntity =
            jpaRepository.findByIdForUpdate(walletId)
                ?: throw PointDomainException("포인트 지갑을 찾을 수 없습니다: walletId=$walletId")
        return PointWalletMapper.of(entity = entity, holdingLimit = resolveHoldingLimit(entity = entity))
    }

    override fun updateBalance(wallet: PointWallet) {
        // memberId를 모르는 호출 경로(배치)를 위한 갱신 — 기존 row에서 memberId/holdingLimitOverride를 그대로 보존한다.
        val existing: PointWalletEntity =
            jpaRepository
                .findById(wallet.id)
                .orElseThrow { PointDomainException("포인트 지갑을 찾을 수 없습니다: walletId=${wallet.id}") }
        jpaRepository.save(
            PointWalletMapper.of(wallet = wallet, memberId = existing.memberId, holdingLimitOverride = existing.holdingLimitOverride),
        )
    }

    private fun resolveHoldingLimit(entity: PointWalletEntity): HoldingLimit =
        entity.holdingLimitOverride
            ?.let { HoldingLimit(it) }
            ?: HoldingLimit(policyRepository.getCurrent().maxHoldingAmount.value)
}
