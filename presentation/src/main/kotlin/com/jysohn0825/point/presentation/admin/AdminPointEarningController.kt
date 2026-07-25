package com.jysohn0825.point.presentation.admin

import com.jysohn0825.point.application.earning.EarnPointDto
import com.jysohn0825.point.application.earning.EarnPointService
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.presentation.dto.request.ManualGrantPointRequest
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
import java.util.UUID

@Tag(name = "Admin Point Earning", description = "관리자 포인트 수기 지급 API")
@RestController
@RequestMapping("/api/v1/admin/members/{memberId}/point-earnings")
class AdminPointEarningController(
    private val earnPointService: EarnPointService,
) {
    @Operation(
        summary = "포인트 수기 지급",
        description = "관리자가 회원에게 포인트를 수기로 지급한다. 수기지급 포인트는 다른 적립과 구분되어 식별된다.",
    )
    @ApiResponse(responseCode = "200", description = "수기 지급 성공")
    @PostMapping
    fun grant(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Valid @RequestBody request: ManualGrantPointRequest,
    ): PointEarningResponse =
        earnPointService
            .earn(
                EarnPointDto(
                    memberId = memberId,
                    amount = request.amount,
                    earnType = EarnType.MANUAL,
                    // 수기 지급은 재시도 멱등성 키가 요청에 없어(관리자 액션은 매번 새 지급으로 취급), 매번 새 참조값을 발급한다.
                    sourceReferenceId = "MANUAL_GRANT:${UUID.randomUUID()}",
                    grantedByAdminId = request.adminId,
                    expirationPeriod = request.expirationDays?.let(::ExpirationPeriod),
                ),
            ).toResponse(memberId)
}
