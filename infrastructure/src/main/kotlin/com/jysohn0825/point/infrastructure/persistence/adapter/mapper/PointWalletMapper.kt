package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity

class PointWalletMapper {
    companion object {
        fun of(entity: PointWalletEntity): PointWallet =
            PointWallet.open(
                id = entity.id,
                holdingLimit = HoldingLimit(entity.holdingLimit),
                balance = Balance(entity.balance),
            )

        fun of(
            wallet: PointWallet,
            memberId: String,
        ): PointWalletEntity =
            PointWalletEntity(
                id = wallet.id,
                memberId = memberId,
                balance = wallet.balance.amount,
                holdingLimit = wallet.holdingLimit.value,
            )
    }
}
