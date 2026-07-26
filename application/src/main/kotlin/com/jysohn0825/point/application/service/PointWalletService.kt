package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.PointWalletResultDto
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.support.lock.DistributedLock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PointWalletService(
    private val walletRepository: PointWalletRepository,
    private val policyRepository: PointPolicyRepository,
) {
    /** 회원당 지갑이 1개인 스키마 제약을 지키기 위해, 동일 회원의 동시 생성 요청을 락으로 직렬화한다. */
    @DistributedLock(key = "'point-wallet-create-lock:' + #memberId")
    @Transactional
    fun createWallet(memberId: String): PointWalletResultDto {
        walletRepository.findByMemberId(memberId)?.let {
            throw PointDomainException("이미 포인트 지갑이 존재하는 회원입니다: memberId=$memberId")
        }

        val policy: PointPolicy = policyRepository.getCurrent()
        val wallet: PointWallet =
            PointWallet.open(
                id = UUID.randomUUID().toString(),
                holdingLimit = HoldingLimit(policy.maxHoldingAmount.value),
            )

        walletRepository.save(wallet = wallet, memberId = memberId)

        return PointWalletResultDto(pointWallet = wallet)
    }

    @Transactional(readOnly = true)
    fun getWallet(memberId: String): PointWalletResultDto {
        val wallet: PointWallet =
            walletRepository.findByMemberId(memberId)
                ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
        return PointWalletResultDto(pointWallet = wallet)
    }
}
