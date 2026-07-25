package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.presentation.dto.response.PointWalletResponse

fun toResponse(
    pointWallet: PointWallet,
    memberId: String,
): PointWalletResponse =
    PointWalletResponse(
        walletId = pointWallet.id,
        memberId = memberId,
        balance = pointWallet.balance.amount,
        holdingLimit = pointWallet.holdingLimit.value,
    )
