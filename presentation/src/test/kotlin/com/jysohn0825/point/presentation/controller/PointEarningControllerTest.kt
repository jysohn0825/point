package com.jysohn0825.point.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointUsageRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.OrderNumber
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.presentation.controller.dto.request.EarnPointRequest
import com.jysohn0825.point.presentation.support.PresentationTestApplication
import com.jysohn0825.point.presentation.support.PresentationTestConfig
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class PointEarningControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val walletRepository: FakePointWalletRepository,
    @Autowired private val earningRepository: FakePointEarningRepository,
    @Autowired private val usageRepository: FakePointUsageRepository,
) : BehaviorSpec({
        beforeEach {
            walletRepository.clear()
            earningRepository.clear()
            usageRepository.clear()
        }

        Given("지갑이 있는 회원일 때") {
            When("포인트 적립을 요청하면") {
                Then("200과 적립 결과가 반환된다") {
                    walletRepository.seed(memberId = "member-earn-1", wallet = pointWallet(id = "wallet-earn-1"))
                    val request: EarnPointRequest = EarnPointRequest(amount = BigDecimal(1_000), sourceReferenceId = "ORDER-1")

                    mockMvc
                        .post("/api/v1/members/member-earn-1/point-earnings") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isOk() }
                            jsonPath("$.memberId") { value("member-earn-1") }
                            jsonPath("$.amount") { value(1_000) }
                            jsonPath("$.earnType") { value("SYSTEM") }
                            jsonPath("$.status") { value("ACTIVE") }
                        }
                }
            }
        }

        Given("지갑이 있는 회원이 유효하지 않은 금액으로 적립을 요청할 때") {
            When("적립을 요청하면") {
                Then("400이 반환된다") {
                    walletRepository.seed(memberId = "member-earn-2", wallet = pointWallet(id = "wallet-earn-2"))
                    val invalidJson: String = """{"amount": -1, "sourceReferenceId": "ORDER-1"}"""

                    mockMvc
                        .post("/api/v1/members/member-earn-2/point-earnings") {
                            contentType = MediaType.APPLICATION_JSON
                            content = invalidJson
                        }.andExpect {
                            status { isBadRequest() }
                        }
                }
            }
        }

        Given("적립건을 보유한 회원일 때") {
            When("적립 취소를 요청하면") {
                Then("취소 상태로 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-earn-3", balance = balance(BigDecimal(1_000)))
                    walletRepository.seed(memberId = "member-earn-3", wallet = wallet)
                    val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .post("/api/v1/members/member-earn-3/point-earnings/${earning.id}/cancellations")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.status") { value("CANCELED") }
                        }
                }
            }
        }

        Given("적립건이 여러 건 있는 회원일 때") {
            When("적립건 목록을 조회하면") {
                Then("보유한 적립건 개수만큼 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-earn-4")
                    walletRepository.seed(memberId = "member-earn-4", wallet = wallet)
                    earningRepository.save(earning = pointEarning(sourceReferenceId = "A"), walletId = wallet.id, policyId = "policy-1")
                    earningRepository.save(earning = pointEarning(sourceReferenceId = "B"), walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .get("/api/v1/members/member-earn-4/point-earnings")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.length()") { value(2) }
                        }
                }
            }
        }

        Given("적립건을 보유한 회원의 적립건 상세를 조회할 때") {
            When("적립건 상세를 조회하면") {
                Then("해당 적립건이 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-earn-5")
                    walletRepository.seed(memberId = "member-earn-5", wallet = wallet)
                    val earning: PointEarning =
                        pointEarning(amount = pointAmount(BigDecimal(700)), sourceReferenceId = "ORDER-5")
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .get("/api/v1/members/member-earn-5/point-earnings/${earning.id}")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.earningId") { value(earning.id) }
                            jsonPath("$.amount") { value(700) }
                        }
                }
            }
        }

        Given("존재하지 않는 적립건일 때") {
            When("적립건 상세를 조회하면") {
                Then("400이 반환된다") {
                    mockMvc
                        .get("/api/v1/members/member-earn-6/point-earnings/no-such-earning")
                        .andExpect {
                            status { isBadRequest() }
                        }
                }
            }
        }

        Given("일부 사용된 적립건을 보유한 회원일 때") {
            When("적립건의 사용 추적을 조회하면") {
                Then("사용 내역이 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-earn-7")
                    walletRepository.seed(memberId = "member-earn-7", wallet = wallet)
                    val earning: PointEarning =
                        pointEarning(amount = pointAmount(BigDecimal(1_000)), sourceReferenceId = "ORDER-7", earnType = EarnType.SYSTEM)
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
                    earning.use(BigDecimal(300))
                    usageRepository.save(
                        usage =
                            PointUsage.use(
                                orderNumber = OrderNumber("A1234"),
                                lines = listOf(UsageLine(earningId = earning.id, amount = BigDecimal(300))),
                            ),
                        walletId = wallet.id,
                    )

                    mockMvc
                        .get("/api/v1/members/member-earn-7/point-earnings/${earning.id}/usage-traces")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$[0].orderNumber") { value("A1234") }
                            jsonPath("$[0].amount") { value(300) }
                        }
                }
            }
        }
    })
