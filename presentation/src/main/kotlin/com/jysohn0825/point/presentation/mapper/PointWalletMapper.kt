package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.presentation.dto.response.PointWalletResponse

fun PointWallet.toResponse(memberId: String): PointWalletResponse =
    PointWalletResponse(
        walletId = id,
        memberId = memberId,
        balance = balance.amount,
        holdingLimit = holdingLimit.value,
    )
