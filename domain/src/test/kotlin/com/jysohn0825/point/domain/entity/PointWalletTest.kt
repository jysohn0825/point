package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.Balance
import com.jysohn0825.point.domain.vo.HoldingLimit
import com.jysohn0825.point.domain.vo.balance
import com.jysohn0825.point.domain.vo.holdingLimit
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointWalletTest :
    BehaviorSpec({
        Given("잔액을 지정하지 않으면") {
            val limit: HoldingLimit = holdingLimit(value = BigDecimal(500))

            When("지갑을 개설하면") {
                val wallet: PointWallet = PointWallet.open(id = "member-7", holdingLimit = limit)

                Then("0원 잔액으로 개설된다") {
                    wallet.id shouldBe "member-7"
                    wallet.holdingLimit shouldBe holdingLimit(value = BigDecimal(500))
                    wallet.balance shouldBe Balance.ZERO
                }
            }
        }

        Given("id가 비어있으면") {
            When("지갑을 개설하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> { pointWallet(id = "") }
                    shouldThrow<IllegalArgumentException> { pointWallet(id = "   ") }
                }
            }
        }

        Given("보유한도 이내의 잔액이 주어지면") {
            When("지갑을 개설하면") {
                val wallet: PointWallet =
                    pointWallet(balance = balance(amount = BigDecimal(100)), holdingLimit = holdingLimit(value = BigDecimal(100)))

                Then("해당 잔액으로 개설된다") {
                    wallet.balance shouldBe balance(amount = BigDecimal(100))
                }
            }
        }

        Given("잔액이 보유한도를 초과하면") {
            When("지갑을 개설하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        pointWallet(balance = balance(amount = BigDecimal(101)), holdingLimit = holdingLimit(value = BigDecimal(100)))
                    }
                }
            }
        }

        Given("한도 이내로 적립 가능한 지갑이 주어지면") {
            val wallet: PointWallet = pointWallet(holdingLimit = holdingLimit(value = BigDecimal(1_000)))

            When("적립하면") {
                wallet.earn(pointAmount(value = BigDecimal(500)))

                Then("잔액이 증가한다") {
                    wallet.balance shouldBe balance(amount = BigDecimal(500))
                }
            }
        }

        Given("한도 초과 임계치에 근접한 잔액을 가진 지갑이 주어지면") {
            val wallet: PointWallet =
                pointWallet(balance = balance(amount = BigDecimal(900)), holdingLimit = holdingLimit(value = BigDecimal(1_000)))

            When("한도를 초과하는 금액을 적립하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalStateException> { wallet.earn(pointAmount(value = BigDecimal(200))) }
                }
            }
        }

        Given("잔액이 있는 지갑이 주어지면") {
            val wallet: PointWallet = pointWallet(balance = balance(amount = BigDecimal(500)))

            When("차감하면") {
                wallet.decrease(pointAmount(value = BigDecimal(200)))

                Then("잔액이 감소한다") {
                    wallet.balance shouldBe balance(amount = BigDecimal(300))
                }
            }
        }

        Given("잔액이 부족한 지갑이 주어지면") {
            val wallet: PointWallet = pointWallet(balance = balance(amount = BigDecimal(100)))

            When("잔액보다 많은 금액을 차감하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> { wallet.decrease(pointAmount(value = BigDecimal(200))) }
                }
            }
        }

        Given("동일한 id를 가진 두 지갑이 주어지면") {
            val walletA: PointWallet = pointWallet(id = "member-1")
            val walletB: PointWallet = pointWallet(id = "member-1")

            When("동등성을 비교하면") {
                Then("서로 동등하다") {
                    (walletA == walletB) shouldBe true
                    walletA.hashCode() shouldBe walletB.hashCode()
                }
            }
        }

        Given("다른 id를 가진 두 지갑이 주어지면") {
            val walletA: PointWallet = pointWallet(id = "member-1")
            val walletB: PointWallet = pointWallet(id = "member-2")

            When("동등성을 비교하면") {
                Then("서로 동등하지 않다") {
                    (walletA == walletB) shouldBe false
                }
            }
        }

        Given("지갑이 주어지면") {
            val wallet: PointWallet = pointWallet()

            When("자기 자신과 비교하면") {
                Then("항상 동등하다") {
                    (wallet == wallet) shouldBe true
                }
            }

            When("PointWallet이 아닌 타입과 비교하면") {
                Then("동등하지 않다") {
                    wallet.equals("not a wallet") shouldBe false
                }
            }
        }
    })
