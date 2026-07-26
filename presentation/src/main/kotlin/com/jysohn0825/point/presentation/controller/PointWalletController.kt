package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.application.service.PointWalletService
import com.jysohn0825.point.presentation.controller.dto.response.PointWalletResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Point Wallet", description = "포인트 지갑 생성/조회 API")
@RestController
@RequestMapping("/api/v1/members/{memberId}/point-wallet")
class PointWalletController(
    private val walletService: PointWalletService,
) {
    @Operation(summary = "포인트 지갑 생성", description = "회원의 포인트 지갑을 최초 생성한다. 이미 지갑이 있으면 예외가 발생한다.")
    @ApiResponse(responseCode = "200", description = "생성 성공")
    @PostMapping
    fun createWallet(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
    ): PointWalletResponse =
        PointWalletResponse.of(
            pointWalletResult = walletService.createWallet(memberId),
            memberId = memberId,
        )

    @Operation(summary = "포인트 지갑 조회", description = "회원의 총 잔액과 보유 한도를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    fun getWallet(
        @Parameter(description = "회원 식별자") @PathVariable memberId: String,
    ): PointWalletResponse =
        PointWalletResponse.of(
            pointWalletResult = walletService.getWallet(memberId),
            memberId = memberId,
        )
}
