package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import com.jysohn0825.point.presentation.dto.response.EarningUsageTraceResponse
import com.jysohn0825.point.presentation.dto.response.PointEarningResponse

fun toResponse(
    pointEarning: PointEarning,
    memberId: String,
): PointEarningResponse =
    PointEarningResponse(
        earningId = pointEarning.id,
        memberId = memberId,
        amount = pointEarning.amount.value,
        remainingAmount = pointEarning.remainingAmount.value,
        earnType = pointEarning.earnType,
        grantedBy = pointEarning.grantedBy?.adminId,
        earnedAt = pointEarning.earnedAt,
        expirationDate = pointEarning.expirationDate.value,
        status = pointEarning.status,
    )

fun toResponse(earningUsageTrace: EarningUsageTrace): EarningUsageTraceResponse =
    EarningUsageTraceResponse(
        orderNumber = earningUsageTrace.orderNumber.value,
        amount = earningUsageTrace.amount,
    )
