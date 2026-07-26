package com.jysohn0825.point.presentation.exception

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.support.lock.LockAcquisitionException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException

class GlobalExceptionHandlerTest :
    BehaviorSpec({
        val handler = GlobalExceptionHandler()

        Given("PointDomainException이 발생했을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> = handler.handlePointDomainException(PointDomainException("적립건을 찾을 수 없습니다."))

                Then("400과 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.BAD_REQUEST
                    response.body?.status shouldBe 400
                    response.body?.error shouldBe "BAD_REQUEST"
                    response.body?.message shouldBe "적립건을 찾을 수 없습니다."
                }
            }
        }

        Given("IllegalArgumentException이 발생했을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> = handler.handleIllegalArgumentException(IllegalArgumentException("잘못된 값입니다."))

                Then("400과 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.BAD_REQUEST
                    response.body?.message shouldBe "잘못된 값입니다."
                }
            }
        }

        Given("요청 본문을 읽을 수 없을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> =
                    handler.handleHttpMessageNotReadableException(HttpMessageNotReadableException("파싱 실패"))

                Then("400과 고정 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.BAD_REQUEST
                    response.body?.message shouldBe "요청 본문을 읽을 수 없습니다."
                }
            }
        }

        Given("동일 key에 대한 진짜 동시(in-flight) 요청으로 분산락 획득에 실패했을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> =
                    handler.handleLockAcquisitionException(LockAcquisitionException("락 획득에 실패했습니다: point-earning-lock:member1"))

                Then("409와 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.CONFLICT
                    response.body?.status shouldBe 409
                    response.body?.error shouldBe "CONFLICT"
                    response.body?.message shouldBe "락 획득에 실패했습니다: point-earning-lock:member1"
                }
            }
        }

        Given("이미 끝난 요청의 재시도가 DB 유니크 제약을 위반했을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> =
                    handler.handleDataIntegrityViolationException(DataIntegrityViolationException("uk_usage_order 위반"))

                Then("409와 고정 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.CONFLICT
                    response.body?.status shouldBe 409
                    response.body?.error shouldBe "CONFLICT"
                    response.body?.message shouldBe "이미 처리되었거나 중복된 요청입니다."
                }
            }
        }

        Given("그 외 예상치 못한 예외가 발생했을 때") {
            When("핸들러가 처리하면") {
                val response: ResponseEntity<ErrorResponse> = handler.handleException(RuntimeException("알 수 없는 오류"))

                Then("500과 에러 메시지가 담긴 응답이 반환된다") {
                    response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                    response.body?.status shouldBe 500
                    response.body?.error shouldBe "INTERNAL_SERVER_ERROR"
                    response.body?.message shouldBe "알 수 없는 오류"
                }
            }
        }
    })
