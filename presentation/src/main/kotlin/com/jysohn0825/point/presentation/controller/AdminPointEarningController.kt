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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
    @ApiResponse(responseCode = "201", description = "수기 지급 성공")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun grant(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: ManualGrantPointRequest,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarningResult = earnPointService.manualEarn(request.to(memberId)),
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
            pointEarningResults = expirationService.expireMemberEarningsNow(memberId),
            memberId = memberId,
        )

    @Operation(
        summary = "특정 적립건 강제 만료",
        description =
            "자연 만료일이 아직 지나지 않았거나 이미 전액 사용된 적립건이라도, 관리자가 지정한 적립건 하나를 " +
                "즉시 만료 처리한다. 잔여액이 있으면 소멸(지갑 차감)시키고, 이미 소진된 적립건은 만료일만 지금으로 " +
                "앞당겨 이후 사용취소 시 만료 판정에 반영되게 한다.",
    )
    @ApiResponse(responseCode = "200", description = "만료 처리 성공")
    @PostMapping("/{earningId}/expiration")
    fun forceExpire(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
        @Parameter(description = "강제 만료 처리할 적립 건 식별자") @PathVariable earningId: String,
    ): PointEarningResponse =
        PointEarningResponse.of(
            pointEarningResult = earnPointService.forceExpireEarning(memberId = memberId, earningId = earningId),
            memberId = memberId,
        )
}
