package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.PointPolicyService
import com.jysohn0825.point.presentation.controller.dto.request.UpsertPointPolicyRequest
import com.jysohn0825.point.presentation.controller.dto.response.PointPolicyResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Point Policy", description = "관리자 포인트 정책 생성/변경 API")
@RestController
@RequestMapping("/api/v1/admin/point-policies")
class AdminPointPolicyController(
    private val policyService: PointPolicyService,
) {
    @Operation(
        summary = "포인트 정책 생성/변경",
        description = "1회 적립 한도·보유 한도·기본 만료일을 새 버전으로 등록한다. appliedAt 미지정 시 즉시 적용된다.",
    )
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun upsert(
        @Valid @RequestBody request: UpsertPointPolicyRequest,
    ): PointPolicyResponse = PointPolicyResponse.of(pointPolicyResult = policyService.createOrUpdate(request.to()))
}
