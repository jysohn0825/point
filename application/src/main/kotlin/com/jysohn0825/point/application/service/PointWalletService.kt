package com.jysohn0825.point.application.service

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointWalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointWalletService(
    private val walletRepository: PointWalletRepository,
) {
    @Transactional(readOnly = true)
    fun getWallet(memberId: String): PointWallet =
        walletRepository.findByMemberId(memberId)
            ?: throw PointDomainException("회원의 포인트 지갑을 찾을 수 없습니다: memberId=$memberId")
}
