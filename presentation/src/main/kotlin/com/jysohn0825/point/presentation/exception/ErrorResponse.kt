package com.jysohn0825.point.presentation.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Schema(description = "에러 응답")
data class ErrorResponse(
    @Schema(description = "HTTP 상태 코드", example = "400")
    val status: Int,
    @Schema(description = "에러 코드", example = "BAD_REQUEST")
    val error: String,
    @Schema(description = "에러 메시지")
    val message: String?,
    @Schema(description = "에러 발생 일시")
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun of(
            status: HttpStatus,
            message: String?,
        ) = ErrorResponse(
            status = status.value(),
            error = status.name,
            message = message,
        )
    }
}
