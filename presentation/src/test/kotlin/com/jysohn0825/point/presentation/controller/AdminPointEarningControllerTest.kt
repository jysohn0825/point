package com.jysohn0825.point.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
class AdminPointEarningControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var walletRepository: FakePointWalletRepository

    @Autowired
    private lateinit var earningRepository: FakePointEarningRepository

    @BeforeEach
    fun setUp() {
        walletRepository.clear()
        earningRepository.clear()
    }

    @Test
    fun `관리자가 포인트를 수기 지급하면 200과 지급 결과가 반환된다`() {
        walletRepository.seed("member-admin-1", pointWallet(id = "wallet-admin-1"))
        val request = ManualGrantPointRequest(amount = BigDecimal(2_000), adminId = "admin-01", expirationDays = 30)

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

    @Test
    fun `이미 만료된 적립건이 있는 회원의 만료를 즉시 처리하면 만료 목록이 반환된다`() {
        val wallet = pointWallet(id = "wallet-admin-2", balance = balance(BigDecimal(500)))
        walletRepository.seed("member-admin-2", wallet)
        val expired =
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

    @Test
    fun `만료 대상이 없는 회원의 만료를 즉시 처리하면 빈 목록이 반환된다`() {
        walletRepository.seed("member-admin-3", pointWallet(id = "wallet-admin-3"))

        mockMvc
            .post("/api/v1/admin/members/member-admin-3/point-earnings/expirations")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `잔여액이 남은 적립건을 강제 만료 처리하면 EXPIRED와 함께 지갑 잔액이 차감된다`() {
        val wallet = pointWallet(id = "wallet-admin-4", balance = balance(BigDecimal(1_000)))
        walletRepository.seed("member-admin-4", wallet)
        val earning = pointEarning(id = "earning-admin-4", amount = pointAmount(BigDecimal(1_000)))
        earningRepository.save(earning = earning, walletId = wallet.id, policyId = "policy-1")

        mockMvc
            .post("/api/v1/admin/members/member-admin-4/point-earnings/earning-admin-4/expiration")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("EXPIRED") }
                jsonPath("$.remainingAmount") { value(0) }
            }
    }

    @Test
    fun `이미 전액 사용된 적립건을 강제 만료 처리하면 EXHAUSTED 상태를 유지한다`() {
        val wallet = pointWallet(id = "wallet-admin-5", balance = balance(BigDecimal(0)))
        walletRepository.seed("member-admin-5", wallet)
        val earning = pointEarning(id = "earning-admin-5", amount = pointAmount(BigDecimal(1_000)))
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
