package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.EarnPointService
import com.jysohn0825.point.presentation.controller.dto.request.EarnPointRequest
import com.jysohn0825.point.presentation.controller.dto.response.EarningUsageTraceResponse
import com.jysohn0825.point.presentation.controller.dto.response.PointEarningResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Point Earning", description = "포인트 적립/적립취소 API")
@RestController
@RequestMapping("/api/v1/members/{memberId}/point-earnings")
class PointEarningController(
    private val earnPointService: EarnPointService,
) {
    @Operation(
        summary = "포인트 적립",
        description = "회원에게 포인트를 적립한다. sourceReferenceId는 재요청 시 중복 적립을 막는 멱등성 키다.",
    )
    @ApiResponse(responseCode = "200", description = "적립 성공")
    @PostMapping
    fun earn(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: EarnPointRequest,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarningResult = earnPointService.systemEarn(request.to(memberId)),
            memberId = memberId,
        )

    @Operation(summary = "포인트 적립 취소", description = "적립한 금액 중 사용되지 않은 적립 건을 전액 취소한다.")
    @ApiResponse(responseCode = "200", description = "적립 취소 성공")
    @PostMapping("/{earningId}/cancellations")
    fun cancel(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "취소할 적립 건 식별자") @PathVariable earningId: String,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarningResult = earnPointService.cancelEarning(memberId = memberId, earningId = earningId),
            memberId = memberId,
        )

    @Operation(summary = "포인트 적립건 목록 조회", description = "회원의 전체 적립건을 상태 무관 최신순으로 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    fun getEarnings(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
    ): List<PointEarningResponse> =
        PointEarningResponse.of(
            pointEarningResults = earnPointService.getEarnings(memberId),
            memberId = memberId,
        )

    @Operation(summary = "포인트 적립건 상세 조회", description = "적립 건 하나의 상세 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{earningId}")
    fun getEarning(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "조회할 적립 건 식별자") @PathVariable earningId: String,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarningResult = earnPointService.getEarning(earningId),
            memberId = memberId,
        )

    @Operation(
        summary = "포인트 적립건 사용 추적 조회",
        description = "이 적립건이 어느 주문에서 얼마나 사용됐는지 1원 단위로 조회한다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{earningId}/usage-traces")
    fun getUsageTraces(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "조회할 적립 건 식별자") @PathVariable earningId: String,
    ): List<EarningUsageTraceResponse> = EarningUsageTraceResponse.of(earningUsageTraceResults = earnPointService.getUsageTraces(earningId))
}
