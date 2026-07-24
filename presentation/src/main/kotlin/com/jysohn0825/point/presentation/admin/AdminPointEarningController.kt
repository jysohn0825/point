package com.jysohn0825.point.presentation.admin

import com.jysohn0825.point.presentation.dto.request.ManualGrantPointRequest
import com.jysohn0825.point.presentation.dto.response.PointEarningResponse
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

@Tag(name = "Admin Point Earning", description = "관리자 포인트 수기 지급 API")
@RestController
@RequestMapping("/api/v1/admin/members/{memberId}/point-earnings")
class AdminPointEarningController {
    @Operation(
        summary = "포인트 수기 지급",
        description = "관리자가 회원에게 포인트를 수기로 지급한다. 수기지급 포인트는 다른 적립과 구분되어 식별된다.",
    )
    @ApiResponse(responseCode = "201", description = "수기 지급 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun grant(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: ManualGrantPointRequest,
    ): PointEarningResponse = TODO("수기 지급 유스케이스 연동 필요")
}
