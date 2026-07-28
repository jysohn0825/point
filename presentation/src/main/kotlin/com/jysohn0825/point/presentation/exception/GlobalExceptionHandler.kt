package com.jysohn0825.point.presentation.exception

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.support.lock.LockAcquisitionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(PointDomainException::class)
    fun handlePointDomainException(e: PointDomainException): ResponseEntity<ErrorResponse> {
        log.warn("PointDomainException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(status = HttpStatus.BAD_REQUEST, message = e.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message: String? = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("MethodArgumentNotValidException: {}", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(status = HttpStatus.BAD_REQUEST, message = message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("HttpMessageNotReadableException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(status = HttpStatus.BAD_REQUEST, message = "요청 본문을 읽을 수 없습니다."))
    }

    /** 동일 key에 대한 진짜 동시(in-flight) 요청이라 분산락 획득에 실패한 경우 — 이미 끝난 요청의 재시도는 락 획득에 성공한다. */
    @ExceptionHandler(LockAcquisitionException::class)
    fun handleLockAcquisitionException(e: LockAcquisitionException): ResponseEntity<ErrorResponse> {
        log.warn("LockAcquisitionException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(status = HttpStatus.CONFLICT, message = e.message))
    }

    /** 이미 끝난 요청의 재시도가 DB 유니크 제약(예: uk_usage_order, uk_wallet_member)을 위반한 경우. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("DataIntegrityViolationException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(status = HttpStatus.CONFLICT, message = "이미 처리되었거나 중복된 요청입니다."))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("IllegalArgumentException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(status = HttpStatus.BAD_REQUEST, message = e.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message))
    }
}
