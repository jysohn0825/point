package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.presentation.dto.request.CancelUsagePointRequest
import com.jysohn0825.point.presentation.dto.request.UsePointRequest
import com.jysohn0825.point.presentation.dto.response.PointUsageCancellationResponse
import com.jysohn0825.point.presentation.dto.response.PointUsageResponse
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

@Tag(name = "Point Usage", description = "포인트 사용/사용취소 API")
@RestController
@RequestMapping("/api/v1/members/{memberId}/point-usages")
class PointUsageController {
    @Operation(summary = "포인트 사용", description = "주문번호와 함께 포인트를 사용한다. 수기지급 포인트가 우선, 만료일이 짧은 순으로 차감된다.")
    @ApiResponse(responseCode = "201", description = "사용 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun use(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: UsePointRequest,
    ): PointUsageResponse = TODO("사용 유스케이스 연동 필요")

    @Operation(
        summary = "포인트 사용취소",
        description = "사용한 금액의 전체 또는 일부를 취소한다. 이미 만료된 적립 건으로 복원해야 하는 경우 신규 적립으로 대체된다.",
    )
    @ApiResponse(responseCode = "200", description = "사용취소 성공")
    @PostMapping("/{usageId}/cancellations")
    fun cancel(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Parameter(description = "취소할 사용 건 식별자") @PathVariable usageId: String,
        @Valid @RequestBody request: CancelUsagePointRequest,
    ): PointUsageCancellationResponse = TODO("사용취소 유스케이스 연동 필요")
}
