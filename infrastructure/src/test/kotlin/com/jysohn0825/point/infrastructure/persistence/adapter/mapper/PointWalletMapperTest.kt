package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.holdingLimit
import com.jysohn0825.point.infrastructure.persistence.entity.PointWalletEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointWalletMapperTest :
    BehaviorSpec({
        Given("지갑 엔티티가 있을 때") {
            val entity: PointWalletEntity =
                PointWalletEntity(id = "wallet-1", memberId = "member-1", balance = BigDecimal(1_400))

            When("정책의 보유한도를 적용해 도메인으로 변환하면") {
                val wallet: PointWallet = PointWalletMapper.of(entity = entity, holdingLimit = holdingLimit(BigDecimal(2_000_000)))

                Then("잔액과 한도가 매핑된다") {
                    wallet.id shouldBe "wallet-1"
                    wallet.balance.amount shouldBe BigDecimal(1_400L)
                    wallet.holdingLimit.value shouldBe BigDecimal(2_000_000)
                }
            }
        }

        Given("도메인 지갑을 저장할 때") {
            val wallet: PointWallet = pointWallet(id = "wallet-2", balance = balance(BigDecimal(700)))

            When("엔티티로 변환하면") {
                val entity: PointWalletEntity = PointWalletMapper.of(wallet = wallet, memberId = "member-2")

                Then("id/memberId/balance가 매핑된다") {
                    entity.id shouldBe "wallet-2"
                    entity.memberId shouldBe "member-2"
                    entity.balance shouldBe BigDecimal(700)
                }
            }
        }
    })
