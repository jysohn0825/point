package com.jysohn0825.point.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jysohn0825.point.domain.repository.FakePointPolicyRepository
import com.jysohn0825.point.presentation.controller.dto.request.UpsertPointPolicyRequest
import com.jysohn0825.point.presentation.support.PresentationTestApplication
import com.jysohn0825.point.presentation.support.PresentationTestConfig
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class AdminPointPolicyControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val policyRepository: FakePointPolicyRepository,
) : BehaviorSpec({
        beforeEach {
            policyRepository.reset()
        }

        Given("관리자가 새 포인트 정책을 등록할 때") {
            val request: UpsertPointPolicyRequest =
                UpsertPointPolicyRequest(
                    maxEarnPerTransaction = BigDecimal(50_000),
                    maxHoldingAmount = BigDecimal(2_000_000),
                    defaultExpirationDays = 180,
                    adminId = "admin-01",
                )

            When("등록을 요청하면") {
                Then("201과 등록된 정책이 반환된다") {
                    mockMvc
                        .post("/api/v1/admin/point-policies") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isCreated() }
                            jsonPath("$.maxEarnPerTransaction") { value(50_000) }
                            jsonPath("$.maxHoldingAmount") { value(2_000_000) }
                            jsonPath("$.defaultExpirationDays") { value(180) }
                        }
                }
            }
        }

        Given("1회 적립 한도가 허용 범위를 벗어나는 정책을 등록할 때") {
            val request: UpsertPointPolicyRequest =
                UpsertPointPolicyRequest(
                    maxEarnPerTransaction = BigDecimal(200_000),
                    maxHoldingAmount = BigDecimal(2_000_000),
                    defaultExpirationDays = 180,
                    adminId = "admin-01",
                )

            When("등록을 요청하면") {
                Then("400과 에러 응답이 반환된다") {
                    mockMvc
                        .post("/api/v1/admin/point-policies") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.status") { value(400) }
                        }
                }
            }
        }
    })
