package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.PointEarningExpirationService
import com.jysohn0825.point.presentation.controller.dto.response.PointEarningExpirationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Point Earning Expiration", description = "포인트 만료 배치 트리거 API")
@RestController
@RequestMapping("/api/v1/admin/point-earnings/expirations")
class AdminPointEarningExpirationController(
    private val expirationService: PointEarningExpirationService,
) {
    @Operation(
        summary = "포인트 만료 배치 실행",
        description =
            "만료일이 지난 전체 지갑의 적립건을 일괄 만료 처리한다. " +
                "외부에서 스케줄링을 하여 호출해야하며, 스프링 배치 기반으로 동작하는게 맞으나 과제 특성상 API로 대체한다.",
    )
    @ApiResponse(responseCode = "200", description = "배치 실행 성공")
    @PostMapping
    fun expireAllDue(): PointEarningExpirationResponse =
        PointEarningExpirationResponse.of(
            processedWalletCount = expirationService.expireAllDue(),
        )
}
