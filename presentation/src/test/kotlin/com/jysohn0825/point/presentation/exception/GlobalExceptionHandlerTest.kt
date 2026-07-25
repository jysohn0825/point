package com.jysohn0825.point.presentation.exception

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
