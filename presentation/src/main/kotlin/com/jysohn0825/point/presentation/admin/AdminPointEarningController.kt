package com.jysohn0825.point.presentation.admin

import com.jysohn0825.point.application.earning.EarnPointDto
import com.jysohn0825.point.application.earning.EarnPointService
import com.jysohn0825.point.application.expiration.PointEarningExpirationService
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

    @Operation(
        summary = "포인트 만료 즉시 처리",
        description = "스케줄러를 기다리지 않고, 이미 만료일이 지난 회원의 적립건을 관리자가 즉시 만료 처리한다.",
    )
    @ApiResponse(responseCode = "200", description = "만료 처리 성공(대상이 없으면 빈 목록)")
    @PostMapping("/expirations")
    fun expireNow(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
    ): List<PointEarningResponse> = expirationService.expireMemberEarningsNow(memberId).map { it.toResponse(memberId) }

    @Operation(
        summary = "포인트 적립건 강제 만료",
        description = "실제 만료일과 무관하게 특정 적립건 하나를 지금 즉시 만료 처리한다. " +
            "아직 유효한 포인트를 소멸시킬 수 있으므로 운영 지원/테스트 목적 등 신중한 사용이 필요하다.",
    )
    @ApiResponse(responseCode = "200", description = "강제 만료 성공")
    @PostMapping("/{earningId}/force-expire")
    fun forceExpire(
        @Parameter(description = "회원(지갑) 식별자") @PathVariable memberId: String,
        @Parameter(description = "강제 만료할 적립 건 식별자") @PathVariable earningId: String,
    ): PointEarningResponse = expirationService.forceExpireEarning(memberId, earningId).toResponse(memberId)
}
