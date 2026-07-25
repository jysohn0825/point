package com.jysohn0825.point.presentation.controller

import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.presentation.support.FakePointWalletRepository
import com.jysohn0825.point.presentation.support.PresentationTestApplication
import com.jysohn0825.point.presentation.support.PresentationTestConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal

@SpringBootTest(classes = [PresentationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PresentationTestConfig::class)
class PointWalletControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var walletRepository: FakePointWalletRepository

    @BeforeEach
    fun setUp() {
        walletRepository.clear()
    }

    @Test
    fun `지갑을 조회하면 잔액과 보유한도가 반환된다`() {
        walletRepository.seed("member-wallet-1", pointWallet(id = "wallet-1", balance = balance(BigDecimal(1_400))))

        mockMvc
            .get("/api/v1/members/member-wallet-1/point-wallet")
            .andExpect {
                status { isOk() }
                jsonPath("$.walletId") { value("wallet-1") }
                jsonPath("$.memberId") { value("member-wallet-1") }
                jsonPath("$.balance") { value(1_400) }
            }
    }

    @Test
    fun `존재하지 않는 회원의 지갑을 조회하면 400과 에러 응답이 반환된다`() {
        mockMvc
            .get("/api/v1/members/no-such-member/point-wallet")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
                jsonPath("$.error") { value("BAD_REQUEST") }
            }
    }
}
