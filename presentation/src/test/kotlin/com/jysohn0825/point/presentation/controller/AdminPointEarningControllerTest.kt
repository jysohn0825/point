package com.jysohn0825.point.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.presentation.controller.dto.request.ManualGrantPointRequest
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
import java.time.LocalDateTime

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class AdminPointEarningControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val walletRepository: FakePointWalletRepository,
    @Autowired private val earningRepository: FakePointEarningRepository,
) : BehaviorSpec({
        beforeEach {
            walletRepository.clear()
            earningRepository.clear()
        }

        Given("지갑이 있는 회원에게 관리자가 포인트를 수기 지급할 때") {
            When("수기 지급을 요청하면") {
                Then("200과 지급 결과가 반환된다") {
                    walletRepository.seed(memberId = "member-admin-1", wallet = pointWallet(id = "wallet-admin-1"))
                    val request: ManualGrantPointRequest =
                        ManualGrantPointRequest(amount = BigDecimal(2_000), adminId = "admin-01", expirationDays = 30)

                    mockMvc
                        .post("/api/v1/admin/members/member-admin-1/point-earnings") {
                            contentType = MediaType.APPLICATION_JSON
                            content = objectMapper.writeValueAsString(request)
                        }.andExpect {
                            status { isOk() }
                            jsonPath("$.amount") { value(2_000) }
                            jsonPath("$.earnType") { value("MANUAL") }
                            jsonPath("$.grantedBy") { value("admin-01") }
                        }
                }
            }
        }

        Given("이미 만료된 적립건이 있는 회원일 때") {
            When("만료를 즉시 처리하면") {
                Then("만료 목록이 반환된다") {
                    val wallet: PointWallet =
                        pointWallet(id = "wallet-admin-2", balance = balance(BigDecimal(500)))
                    walletRepository.seed(memberId = "member-admin-2", wallet = wallet)
                    val expired: PointEarning =
                        pointEarning(
                            amount = pointAmount(BigDecimal(500)),
                            earnedAt = LocalDateTime.now().minusDays(10),
                            period = expirationPeriod(1),
                        )
                    earningRepository.save(earning = expired, walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .post("/api/v1/admin/members/member-admin-2/point-earnings/expirations")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.length()") { value(1) }
                            jsonPath("$[0].status") { value("EXPIRED") }
                        }
                }
            }
        }

        Given("만료 대상이 없는 회원일 때") {
            When("만료를 즉시 처리하면") {
                Then("빈 목록이 반환된다") {
                    walletRepository.seed(memberId = "member-admin-3", wallet = pointWallet(id = "wallet-admin-3"))

                    mockMvc
                        .post("/api/v1/admin/members/member-admin-3/point-earnings/expirations")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.length()") { value(0) }
                        }
                }
            }
        }

        Given("잔여액이 남은 적립건을 가진 회원일 때") {
            When("해당 적립건을 강제 만료 처리하면") {
                Then("EXPIRED와 함께 지갑 잔액이 차감된다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-admin-4", balance = balance(BigDecimal(1_000)))
                    walletRepository.seed(memberId = "member-admin-4", wallet = wallet)
                    val earning: PointEarning = pointEarning(id = "earning-admin-4", amount = pointAmount(BigDecimal(1_000)))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .post("/api/v1/admin/members/member-admin-4/point-earnings/earning-admin-4/expiration")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.status") { value("EXPIRED") }
                            jsonPath("$.remainingAmount") { value(0) }
                        }
                }
            }
        }

        Given("이미 전액 사용된 적립건을 가진 회원일 때") {
            When("해당 적립건을 강제 만료 처리하면") {
                Then("EXHAUSTED 상태를 유지한다") {
                    val wallet: PointWallet = pointWallet(id = "wallet-admin-5", balance = balance(BigDecimal(0)))
                    walletRepository.seed(memberId = "member-admin-5", wallet = wallet)
                    val earning: PointEarning = pointEarning(id = "earning-admin-5", amount = pointAmount(BigDecimal(1_000)))
                    earning.use(BigDecimal(1_000))
                    earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")

                    mockMvc
                        .post("/api/v1/admin/members/member-admin-5/point-earnings/earning-admin-5/expiration")
                        .andExpect {
                            status { isOk() }
                            jsonPath("$.status") { value("EXHAUSTED") }
                        }
                }
            }
        }
    })
