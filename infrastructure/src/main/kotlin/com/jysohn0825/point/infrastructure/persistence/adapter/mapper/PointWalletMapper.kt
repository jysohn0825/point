package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import java.math.BigDecimal

class PointWalletMapper {
    companion object {
        fun of(
            entity: PointWalletEntity,
            holdingLimit: HoldingLimit,
        ): PointWallet =
            PointWallet.open(
                id = entity.id,
                holdingLimit = holdingLimit,
                balance = Balance(BigDecimal.valueOf(entity.balance)),
            )

        fun of(
            wallet: PointWallet,
            memberId: String,
            holdingLimitOverride: Long?,
        ): PointWalletEntity =
            PointWalletEntity(
                id = wallet.id,
                memberId = memberId,
                balance = wallet.balance.amount.longValueExact(),
                holdingLimitOverride = holdingLimitOverride,
            )
    }
}
