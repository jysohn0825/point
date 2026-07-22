package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.domain.vo.holdingLimit

fun pointWallet(
    id: String = "1",
    holdingLimit: HoldingLimit = holdingLimit(),
    balance: Balance = Balance.ZERO,
): PointWallet = PointWallet.open(id = id, holdingLimit = holdingLimit, balance = balance)
