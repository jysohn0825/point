package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointPolicyEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.math.BigDecimal
import java.time.LocalDateTime

class PointPolicyMapperTest :
    BehaviorSpec({
        Given("정책 엔티티가 있을 때") {
            val entity: PointPolicyEntity =
                PointPolicyEntity(
                    id = "policy-1",
                    policyVersion = 3,
                    maxEarnPerTransaction = 50_000,
                    maxHoldingAmount = 1_000_000L,
                    defaultExpirationDays = 365,
                    appliedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    createdByAdminId = "admin-01",
                )

            When("도메인으로 변환하면") {
                val policy: PointPolicy = PointPolicyMapper.of(entity)

                Then("모든 필드가 그대로 매핑된다") {
                    policy.id shouldBe "policy-1"
                    policy.maxEarnPerTransaction.value shouldBe BigDecimal(50_000)
                    policy.maxHoldingAmount.value shouldBe BigDecimal(1_000_000L)
                    policy.defaultExpirationPeriod.days shouldBe 365L
                }
            }
        }

        Given("도메인 정책을 신규 버전으로 저장할 때") {
            val policy: PointPolicy =
                pointPolicy(
                    maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(30_000)),
                    maxHoldingAmount = maxHoldingAmount(BigDecimal(500_000)),
                    defaultExpirationPeriod = expirationPeriod(180),
                )

            When("엔티티로 변환하면") {
                val entity: PointPolicyEntity =
                    PointPolicyMapper.of(
                        policy = policy,
                        policyVersion = 4,
                        appliedAt = LocalDateTime.of(2026, 6, 1, 0, 0),
                        createdByAdminId = "admin-02",
                    )

                Then("새 row 식별자가 채번되고 나머지 필드가 매핑된다") {
                    entity.id.shouldNotBeBlank()
                    entity.policyVersion shouldBe 4
                    entity.maxEarnPerTransaction shouldBe 30_000
                    entity.maxHoldingAmount shouldBe 500_000L
                    entity.defaultExpirationDays shouldBe 180
                    entity.appliedAt shouldBe LocalDateTime.of(2026, 6, 1, 0, 0)
                    entity.createdByAdminId shouldBe "admin-02"
                }
            }
        }
    })
