package com.jysohn0825.point.presentation.controller.dto.response

data class PointEarningExpirationResponse(
    val processedWalletCount: Int,
) {
    companion object {
        fun of(processedWalletCount: Int): PointEarningExpirationResponse =
            PointEarningExpirationResponse(processedWalletCount = processedWalletCount)
    }
}
