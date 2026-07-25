package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.presentation.dto.response.PointEarningResponse

fun PointEarning.toResponse(memberId: String): PointEarningResponse =
    PointEarningResponse(
        earningId = id,
        memberId = memberId,
        amount = amount.value,
        remainingAmount = remainingAmount.value,
        earnType = earnType,
        grantedBy = grantedBy?.adminId,
        earnedAt = earnedAt,
        expirationDate = expirationDate.value,
        status = status,
    )
