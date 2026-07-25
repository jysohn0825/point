package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.earning.EarnPointDto
import com.jysohn0825.point.application.earning.EarnPointService
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.presentation.dto.request.EarnPointRequest
import com.jysohn0825.point.presentation.dto.response.PointEarningResponse
import com.jysohn0825.point.presentation.mapper.toResponse
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
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: EarnPointRequest,
    ): PointEarningResponse =
        earnPointService
            .earn(
                EarnPointDto(
                    memberId = memberId,
                    amount = request.amount,
                    earnType = EarnType.SYSTEM,
                    sourceReferenceId = request.sourceReferenceId,
                ),
            ).toResponse(memberId)

    @Operation(summary = "포인트 적립 취소", description = "적립한 금액 중 사용되지 않은 적립 건을 전액 취소한다.")
    @ApiResponse(responseCode = "200", description = "적립 취소 성공")
    @PostMapping("/{earningId}/cancellations")
    fun cancel(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Parameter(description = "취소할 적립 건 식별자") @PathVariable earningId: String,
    ): PointEarningResponse = earnPointService.cancelEarning(memberId, earningId).toResponse(memberId)
}
