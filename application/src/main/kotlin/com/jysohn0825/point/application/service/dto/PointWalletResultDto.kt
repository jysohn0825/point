package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.entity.PointWallet

data class PointWalletResultDto(
    val pointWallet: PointWallet,
) {
    companion object {
        fun of(pointWallet: PointWallet): PointWalletResultDto = PointWalletResultDto(pointWallet = pointWallet)
    }
}
