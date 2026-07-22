package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.vo.defaultExpirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PointPolicyTest : BehaviorSpec({

    given("포인트 정책 픽스처가 주어졌을 때") {
        `when`("픽스처 기본값으로 생성하면") {
            val sut = pointPolicy()

            then("모든 필드가 기본값으로 설정된다") {
                sut.id shouldBe "point-policy-1"
                sut.maxEarnPerTransaction shouldBe maxEarnPerTransaction()
                sut.maxHoldingAmount shouldBe maxHoldingAmount()
                sut.defaultExpirationPeriod shouldBe defaultExpirationPeriod()
            }
        }
    }

    given("정책이 이미 생성되어 있을 때") {
        `when`("일부 필드만 수정하면") {
            val sut = pointPolicy()
            val newMaxEarnPerTransaction = maxEarnPerTransaction(value = 80_000L)

            sut.update(maxEarnPerTransaction = newMaxEarnPerTransaction)

            then("수정한 필드만 반영되고 나머지 필드는 유지된다") {
                sut.maxEarnPerTransaction shouldBe newMaxEarnPerTransaction
                sut.maxHoldingAmount shouldBe maxHoldingAmount()
                sut.defaultExpirationPeriod shouldBe defaultExpirationPeriod()
            }
        }

        `when`("모든 필드를 수정하면") {
            val sut = pointPolicy()
            val newMaxEarnPerTransaction = maxEarnPerTransaction(value = 70_000L)
            val newMaxHoldingAmount = maxHoldingAmount(value = 2_000_000L)
            val newDefaultExpirationPeriod = defaultExpirationPeriod(days = 730)

            sut.update(
                maxEarnPerTransaction = newMaxEarnPerTransaction,
                maxHoldingAmount = newMaxHoldingAmount,
                defaultExpirationPeriod = newDefaultExpirationPeriod,
            )

            then("모든 필드가 갱신된다") {
                sut.maxEarnPerTransaction shouldBe newMaxEarnPerTransaction
                sut.maxHoldingAmount shouldBe newMaxHoldingAmount
                sut.defaultExpirationPeriod shouldBe newDefaultExpirationPeriod
            }
        }
    }

    given("두 정책의 동등성을 비교할 때") {
        `when`("자기 자신과 비교하면") {
            val sut = pointPolicy(id = "policy-1")

            then("같다고 판단한다") {
                (sut == sut) shouldBe true
                sut.equals(sut) shouldBe true
            }
        }

        `when`("id는 같고 다른 필드 값이 다르면") {
            val sut = pointPolicy(id = "policy-1", maxEarnPerTransaction = maxEarnPerTransaction(value = 10_000L))
            val other = pointPolicy(id = "policy-1", maxEarnPerTransaction = maxEarnPerTransaction(value = 90_000L))

            then("같은 엔티티로 판단하고 해시코드도 같다") {
                sut shouldBe other
                sut.hashCode() shouldBe other.hashCode()
            }
        }

        `when`("id가 다르면") {
            val sut = pointPolicy(id = "policy-1")
            val other = pointPolicy(id = "policy-2")

            then("모든 필드 값이 같아도 다른 엔티티로 판단한다") {
                (sut == other) shouldBe false
            }
        }

        `when`("PointPolicy가 아닌 다른 타입과 비교하면") {
            val sut = pointPolicy(id = "policy-1")

            then("같지 않다고 판단한다") {
                sut.equals("policy-1") shouldBe false
                sut.equals(null) shouldBe false
            }
        }
    }
})
