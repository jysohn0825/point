package com.jysohn0825.point.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointUsage
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointUsageRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.domain.vo.usageLine
import com.jysohn0825.point.presentation.controller.dto.request.CancelUsagePointRequest
import com.jysohn0825.point.presentation.controller.dto.request.UsePointRequest
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
import java.time.LocalDateTime

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class PointUsageControllerTest(
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

        Given("사용 가능한 적립건을 보유한 회원일 때") {
            When("포인트 사용을 요청하면") {
                Then("200과 사용 결과가 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-1", balance = balance(BigDecimal(1_000)))
                    walletRepository.seed(memberId = "member-use-1", wallet = wallet)
                    val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
                    val request: UsePointRequest = UsePointRequest(orderNumber = "ORDER-USE-1", amount = BigDecimal(300))

                    mockMvc
                        .post("/api/v1/members/member-use-1/point-usages") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isOk() }
                            jsonPath("$.memberId") { value("member-use-1") }
                            jsonPath("$.orderNumber") { value("ORDER-USE-1") }
                            jsonPath("$.totalAmount") { value(300) }
                        }
                }
            }
        }

        Given("가용 포인트가 부족한 회원일 때") {
            When("가용 포인트보다 많이 사용하려 하면") {
                Then("400이 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-2", balance = balance(BigDecimal(100)))
                    walletRepository.seed(memberId = "member-use-2", wallet = wallet)
                    val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(100)))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
                    val request: UsePointRequest = UsePointRequest(orderNumber = "ORDER-USE-2", amount = BigDecimal(500))

                    mockMvc
                        .post("/api/v1/members/member-use-2/point-usages") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isBadRequest() }
                        }
                }
            }
        }

        Given("사용건을 보유한 회원일 때") {
            When("사용취소를 요청하면") {
                Then("취소 결과가 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-3", balance = balance(BigDecimal(700)))
                    walletRepository.seed(memberId = "member-use-3", wallet = wallet)
                    val earning: PointEarning = pointEarning(id = "earning-use-3", amount = pointAmount(BigDecimal(1_000)))
                    earning.use(BigDecimal(300))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")
                    val usage: PointUsage =
                        pointUsage(lines = listOf(usageLine(earningId = earning.id, amount = BigDecimal(300))))
                    usageRepository.save(usage = usage, walletId = wallet.id)
                    val request: CancelUsagePointRequest = CancelUsagePointRequest(requestId = "cancel-req-use-3", amount = null)

                    mockMvc
                        .post("/api/v1/members/member-use-3/point-usages/${usage.id}/cancellations") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isOk() }
                            jsonPath("$.usageId") { value(usage.id) }
                            jsonPath("$.cancelledAmount") { value(300) }
                        }
                }
            }
        }

        Given("이미 만료된 적립건에서 차감된 사용건을 보유한 회원일 때") {
            When("해당 사용을 취소하면") {
                Then("신규 재적립건이 응답에 포함되고, 조회 시에도 취소 내역이 유지된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-3b", balance = balance(BigDecimal(600)))
                    walletRepository.seed(memberId = "member-use-3b", wallet = wallet)
                    val expiredEarning: PointEarning =
                        pointEarning(
                            id = "earning-use-3b",
                            amount = pointAmount(BigDecimal(1_000)),
                            earnedAt = LocalDateTime.now().minusDays(10),
                            period = expirationPeriod(1),
                        )
                    expiredEarning.use(BigDecimal(400))
                    earningRepository.save(earning = expiredEarning, walletId = wallet.id, policyId = "policy-1")
                    val usage: PointUsage =
                        pointUsage(lines = listOf(usageLine(earningId = expiredEarning.id, amount = BigDecimal(400))))
                    usageRepository.save(usage = usage, walletId = wallet.id)

                    mockMvc
                        .post("/api/v1/members/member-use-3b/point-usages/${usage.id}/cancellations") {
                            contentType = MediaType.APPLICATION_JSON
                            content =
                                objectMapper.writeValueAsString(
                                    CancelUsagePointRequest(requestId = "cancel-req-use-3b", amount = null),
                                )
                        }.andExpect {
                            status { isOk() }
                            jsonPath("$.reEarnings.length()") { value(1) }
                            jsonPath("$.reEarnings[0].amount") { value(400) }
                            jsonPath("$.cancellationLines[0].restorationType") { value("RE_EARNED") }
                        }

                    mockMvc
                        .get("/api/v1/members/member-use-3b/point-usages/${usage.id}")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.cancellationLines.length()") { value(1) }
                            jsonPath("$.cancellationLines[0].restorationType") { value("RE_EARNED") }
                        }
                }
            }
        }

        Given("사용건이 있는 회원일 때") {
            When("사용건 목록을 조회하면") {
                Then("보유한 사용건 개수만큼 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-4")
                    walletRepository.seed(memberId = "member-use-4", wallet = wallet)
                    usageRepository.save(usage = pointUsage(), walletId = wallet.id)

                    mockMvc
                        .get("/api/v1/members/member-use-4/point-usages")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.length()") { value(1) }
                        }
                }
            }
        }

        Given("사용건 상세를 조회할 회원일 때") {
            When("사용건 상세를 조회하면") {
                Then("해당 사용건이 반환된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-use-5")
                    walletRepository.seed(memberId = "member-use-5", wallet = wallet)
                    val usage: PointUsage =
                        pointUsage(lines = listOf(usageLine(earningId = "earning-5", amount = BigDecimal(400))))
                    usageRepository.save(usage = usage, walletId = wallet.id)

                    mockMvc
                        .get("/api/v1/members/member-use-5/point-usages/${usage.id}")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.usageId") { value(usage.id) }
                            jsonPath("$.totalAmount") { value(400) }
                        }
                }
            }
        }

        Given("존재하지 않는 사용건일 때") {
            When("사용건 상세를 조회하면") {
                Then("400이 반환된다") {
                    mockMvc
                        .get("/api/v1/members/member-use-6/point-usages/no-such-usage")
                        .andExpect {
                            status { isBadRequest() }
                        }
                }
            }
        }
    })
