package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.EarnPointService
import com.jysohn0825.point.application.service.PointEarningExpirationService
import com.jysohn0825.point.presentation.controller.dto.request.ManualGrantPointRequest
import com.jysohn0825.point.presentation.controller.dto.response.PointEarningResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Point Earning", description = "관리자 포인트 수기 지급/만료 API")
@RestController
@RequestMapping("/api/v1/admin/members/{memberId}/point-earnings")
class AdminPointEarningController(
    private val earnPointService: EarnPointService,
    private val expirationService: PointEarningExpirationService,
) {
    @Operation(
        summary = "포인트 수기 지급",
        description = "관리자가 회원에게 포인트를 수기로 지급한다. 수기지급 포인트는 다른 적립과 구분되어 식별된다.",
    )
    @ApiResponse(responseCode = "200", description = "수기 지급 성공")
    @PostMapping
    fun grant(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: ManualGrantPointRequest,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarning = earnPointService.earn(request.to(memberId)),
            memberId = memberId,
        )

    @Operation(
        summary = "포인트 만료 즉시 처리",
        description = "스케줄러를 기다리지 않고, 이미 만료일이 지난 회원의 적립건을 관리자가 즉시 만료 처리한다.",
    )
    @ApiResponse(responseCode = "200", description = "만료 처리 성공(대상이 없으면 빈 목록)")
    @PostMapping("/expirations")
    fun expireNow(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
    ): List<PointEarningResponse> =
        PointEarningResponse.of(
            pointEarnings = expirationService.expireMemberEarningsNow(memberId),
            memberId = memberId,
        )
}
