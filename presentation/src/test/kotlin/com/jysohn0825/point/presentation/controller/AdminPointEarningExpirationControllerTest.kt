package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.repository.FakePointEarningRepository
import com.jysohn0825.point.domain.repository.FakePointWalletRepository
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.presentation.support.PresentationTestApplication
import com.jysohn0825.point.presentation.support.PresentationTestConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class AdminPointEarningExpirationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

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
    fun `만료 대상 적립건이 있는 지갑이 있으면 배치가 처리된 지갑 수를 반환한다`() {
        val wallet = pointWallet(id = "wallet-batch-1", balance = balance(BigDecimal(500)))
        walletRepository.seed("member-batch-1", wallet)
        val expired =
            pointEarning(
                amount = pointAmount(BigDecimal(500)),
                earnedAt = LocalDateTime.now().minusDays(10),
                period = expirationPeriod(1),
            )
        earningRepository.save(earning = expired, walletId = wallet.id, policyId = "policy-1")

        mockMvc
            .post("/api/v1/admin/point-earnings/expirations")
            .andExpect {
                status { isOk() }
                jsonPath("$.processedWalletCount") { value(1) }
            }
    }

    @Test
    fun `만료 대상이 없으면 처리된 지갑 수는 0이다`() {
        mockMvc
            .post("/api/v1/admin/point-earnings/expirations")
            .andExpect {
                status { isOk() }
                jsonPath("$.processedWalletCount") { value(0) }
            }
    }
}
