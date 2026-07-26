package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.UsePointService
import com.jysohn0825.point.presentation.controller.dto.request.CancelUsagePointRequest
import com.jysohn0825.point.presentation.controller.dto.request.UsePointRequest
import com.jysohn0825.point.presentation.controller.dto.response.PointUsageCancellationResponse
import com.jysohn0825.point.presentation.controller.dto.response.PointUsageDetailResponse
import com.jysohn0825.point.presentation.controller.dto.response.PointUsageResponse
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

@Tag(name = "Point Usage", description = "포인트 사용/사용취소 API")
@RestController
@RequestMapping("/api/v1/members/{memberId}/point-usages")
class PointUsageController(
    private val usePointService: UsePointService,
) {
    @Operation(summary = "포인트 사용", description = "주문번호와 함께 포인트를 사용한다. 수기지급 포인트가 우선, 만료일이 짧은 순으로 차감된다.")
    @ApiResponse(responseCode = "200", description = "사용 성공")
    @PostMapping
    fun use(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: UsePointRequest,
    ): PointUsageResponse = PointUsageResponse.of(pointUsageResult = usePointService.use(request.to(memberId)), memberId = memberId)

    @Operation(
        summary = "포인트 사용취소",
        description = "사용한 금액의 전체 또는 일부를 취소한다. 이미 만료된 적립 건으로 복원해야 하는 경우 신규 적립으로 대체된다.",
    )
    @ApiResponse(responseCode = "200", description = "사용취소 성공")
    @PostMapping("/{usageId}/cancellations")
    fun cancel(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "취소할 사용 건 식별자") @PathVariable usageId: String,
        @Valid @RequestBody request: CancelUsagePointRequest,
    ): PointUsageCancellationResponse =
        PointUsageCancellationResponse.of(
            cancelUsagePointResult =
                usePointService.cancelUsage(request.to(memberId, usageId)),
            memberId = memberId,
        )

    @Operation(summary = "포인트 사용건 목록 조회", description = "회원의 전체 사용건을 최신순으로 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    fun getUsages(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
    ): List<PointUsageResponse> = PointUsageResponse.of(pointUsageResults = usePointService.getUsages(memberId), memberId = memberId)

    @Operation(summary = "포인트 사용건 상세 조회", description = "사용 건 하나의 상세 정보를 취소 이력과 함께 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/point-usages/{usageId}")
    fun getUsage(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "조회할 사용 건 식별자") @PathVariable usageId: String,
    ): PointUsageDetailResponse = PointUsageDetailResponse.of(pointUsageResult = usePointService.getUsage(usageId), memberId = memberId)
}
