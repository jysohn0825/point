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

@Repository
class PointWalletPersistenceAdapter(
    private val jpaRepository: PointWalletJpaRepository,
    private val policyRepository: PointPolicyRepository,
) : PointWalletRepository {
    override fun findByMemberIdForUpdate(memberId: String): PointWallet {
        val entity: PointWalletEntity =
            jpaRepository.findByMemberIdForUpdate(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return PointWalletMapper.of(entity = entity, holdingLimit = currentHoldingLimit())
    }

    override fun save(
        wallet: PointWallet,
        memberId: String,
    ) {
        jpaRepository.save(PointWalletMapper.of(wallet = wallet, memberId = memberId))
    }

    override fun findByMemberId(memberId: String): PointWallet? =
        jpaRepository.findByMemberId(memberId)?.let { PointWalletMapper.of(entity = it, holdingLimit = currentHoldingLimit()) }

    private fun currentHoldingLimit(): HoldingLimit = HoldingLimit(policyRepository.getCurrent().maxHoldingAmount.value)
}
