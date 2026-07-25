package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.application.usage.CancelUsagePointResult
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.presentation.dto.response.CancellationLineResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageCancellationResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageDetailResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageResponse
import com.jysohn0825.point.presentation.dto.response.UsageLineResponse

fun PointUsage.toResponse(memberId: String): PointUsageResponse =
    PointUsageResponse(
        usageId = id,
        memberId = memberId,
        orderNumber = orderNumber.value,
        totalAmount = totalAmount,
        lines = lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
        status = status,
    )

fun PointUsage.toDetailResponse(memberId: String): PointUsageDetailResponse =
    PointUsageDetailResponse(
        usageId = id,
        memberId = memberId,
        orderNumber = orderNumber.value,
        totalAmount = totalAmount,
        cancelledAmount = cancelledAmount,
        remainingAmount = remainingAmount,
        lines = lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
        cancellationLines =
            cancellationLines.map {
                CancellationLineResponse(
                    earningId = it.originalLine.earningId,
                    restoredAmount = it.restoredAmount,
                    restorationType = it.restorationType,
                )
            },
        status = status,
    )

fun CancelUsagePointResult.toResponse(memberId: String): PointUsageCancellationResponse =
    PointUsageCancellationResponse(
        usageId = usage.id,
        cancelledAmount = requestedLines.sumOf { it.restoredAmount },
        remainingUsageAmount = usage.remainingAmount,
        cancellationLines =
            requestedLines.map {
                CancellationLineResponse(
                    earningId = it.originalLine.earningId,
                    restoredAmount = it.restoredAmount,
                    restorationType = it.restorationType,
                )
            },
        reEarnings = reEarnings.map { it.toResponse(memberId) },
    )
