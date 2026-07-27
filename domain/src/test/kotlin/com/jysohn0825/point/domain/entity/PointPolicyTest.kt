package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointPolicyTest :
    BehaviorSpec({

        given("포인트 정책 픽스처가 주어졌을 때") {
            `when`("픽스처 기본값으로 생성하면") {
                val sut: PointPolicy = pointPolicy()

                then("모든 필드가 기본값으로 설정된다") {
                    sut.id shouldBe "point-policy-1"
                    sut.maxEarnPerTransaction shouldBe maxEarnPerTransaction()
                    sut.maxHoldingAmount shouldBe maxHoldingAmount()
                    sut.defaultExpirationPeriod shouldBe expirationPeriod()
                }
            }
        }

        given("1회 적립 한도가 10,000원인 정책이 있을 때") {
            val sut: PointPolicy = pointPolicy(maxEarnPerTransaction = maxEarnPerTransaction(value = BigDecimal(10_000)))

            `when`("한도 이하 금액을 검증하면") {
                then("예외가 발생하지 않는다") {
                    shouldNotThrowAny { sut.validateEarnAmount(BigDecimal(10_000)) }
                }
            }

            `when`("한도를 초과하는 금액을 검증하면") {
                then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { sut.validateEarnAmount(BigDecimal(10_001)) }
                }
            }
        }

        given("두 정책의 동등성을 비교할 때") {
            `when`("자기 자신과 비교하면") {
                val sut: PointPolicy = pointPolicy(id = "policy-1")

                then("같다고 판단한다") {
                    (sut == sut) shouldBe true
                    sut.equals(sut) shouldBe true
                }
            }

            `when`("id는 같고 다른 필드 값이 다르면") {
                val sut: PointPolicy =
                    pointPolicy(id = "policy-1", maxEarnPerTransaction = maxEarnPerTransaction(value = BigDecimal(10_000)))
                val other: PointPolicy =
                    pointPolicy(id = "policy-1", maxEarnPerTransaction = maxEarnPerTransaction(value = BigDecimal(90_000)))

                then("같은 엔티티로 판단하고 해시코드도 같다") {
                    sut shouldBe other
                    sut.hashCode() shouldBe other.hashCode()
                }
            }

            `when`("id가 다르면") {
                val sut: PointPolicy = pointPolicy(id = "policy-1")
                val other: PointPolicy = pointPolicy(id = "policy-2")

                then("모든 필드 값이 같아도 다른 엔티티로 판단한다") {
                    (sut == other) shouldBe false
                }
            }

            `when`("PointPolicy가 아닌 다른 타입과 비교하면") {
                val sut: PointPolicy = pointPolicy(id = "policy-1")

                then("같지 않다고 판단한다") {
                    sut.equals("policy-1") shouldBe false
                    sut.equals(null) shouldBe false
                }
            }
        }
    })
