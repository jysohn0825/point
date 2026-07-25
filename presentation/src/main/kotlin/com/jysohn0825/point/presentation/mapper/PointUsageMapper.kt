package com.jysohn0825.point.presentation.mapper

import com.jysohn0825.point.application.usage.CancelUsagePointResult
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.presentation.dto.response.CancellationLineResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageCancellationResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageDetailResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageResponse
import com.jysohn0825.point.presentation.dto.response.UsageLineResponse

fun toResponse(
    pointUsage: PointUsage,
    memberId: String,
): PointUsageResponse =
    PointUsageResponse(
        usageId = pointUsage.id,
        memberId = memberId,
        orderNumber = pointUsage.orderNumber.value,
        totalAmount = pointUsage.totalAmount,
        lines = pointUsage.lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
        status = pointUsage.status,
    )

fun toDetailResponse(
    pointUsage: PointUsage,
    memberId: String,
): PointUsageDetailResponse =
    PointUsageDetailResponse(
        usageId = pointUsage.id,
        memberId = memberId,
        orderNumber = pointUsage.orderNumber.value,
        totalAmount = pointUsage.totalAmount,
        cancelledAmount = pointUsage.cancelledAmount,
        remainingAmount = pointUsage.remainingAmount,
        lines = pointUsage.lines.map { UsageLineResponse(earningId = it.earningId, amount = it.amount) },
        cancellationLines =
            pointUsage.cancellationLines.map {
                CancellationLineResponse(
                    earningId = it.originalLine.earningId,
                    restoredAmount = it.restoredAmount,
                    restorationType = it.restorationType,
                )
            },
        status = pointUsage.status,
    )

fun toResponse(
    cancelUsagePointResult: CancelUsagePointResult,
    memberId: String,
): PointUsageCancellationResponse =
    PointUsageCancellationResponse(
        usageId = cancelUsagePointResult.usage.id,
        cancelledAmount = cancelUsagePointResult.requestedLines.sumOf { it.restoredAmount },
        remainingUsageAmount = cancelUsagePointResult.usage.remainingAmount,
        cancellationLines =
            cancelUsagePointResult.requestedLines.map {
                CancellationLineResponse(
                    earningId = it.originalLine.earningId,
                    restoredAmount = it.restoredAmount,
                    restorationType = it.restorationType,
                )
            },
        reEarnings = cancelUsagePointResult.reEarnings.map { toResponse(pointEarning = it, memberId = memberId) },
    )
