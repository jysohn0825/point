package com.jysohn0825.point.application.service

import com.jysohn0825.point.application.service.dto.UpsertPointPolicyDto
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

private class FakePointPolicyServiceRepository(
    var policy: PointPolicy,
) : PointPolicyRepository {
    var savedAppliedAt: LocalDateTime? = null
    var savedCreatedByAdminId: String? = null

    override fun getCurrent(): PointPolicy = policy

    override fun save(
        policy: PointPolicy,
        appliedAt: LocalDateTime,
        createdByAdminId: String,
    ) {
        this.policy = policy
        this.savedAppliedAt = appliedAt
        this.savedCreatedByAdminId = createdByAdminId
    }
}

class PointPolicyServiceTest :
    BehaviorSpec({
        Given("정책이 아직 등록되지 않았을 때") {
            val policyRepository: FakePointPolicyServiceRepository = FakePointPolicyServiceRepository(policy = pointPolicy())
            val sut: PointPolicyService = PointPolicyService(policyRepository)

            When("관리자가 새 정책을 등록하면") {
                val appliedAt: LocalDateTime = LocalDateTime.now()
                val result: PointPolicy =
                    sut.createOrUpdate(
                        dto =
                            UpsertPointPolicyDto(
                                maxEarnPerTransaction = BigDecimal(50_000),
                                maxHoldingAmount = BigDecimal(1_000_000),
                                defaultExpirationDays = 180L,
                                appliedAt = appliedAt,
                                createdByAdminId = "admin-01",
                            ),
                    )

                Then("등록한 값대로 정책이 생성되어 반환된다") {
                    result.maxEarnPerTransaction.value shouldBe BigDecimal(50_000)
                    result.maxHoldingAmount.value shouldBe BigDecimal(1_000_000)
                    result.defaultExpirationPeriod.days shouldBe 180L
                }

                Then("repository에도 적용 시각과 등록 관리자가 함께 저장된다") {
                    policyRepository.policy.maxEarnPerTransaction.value shouldBe BigDecimal(50_000)
                    policyRepository.savedAppliedAt shouldBe appliedAt
                    policyRepository.savedCreatedByAdminId shouldBe "admin-01"
                }
            }
        }

        Given("이미 적용 중인 정책이 있을 때") {
            val policyRepository: FakePointPolicyServiceRepository = FakePointPolicyServiceRepository(policy = pointPolicy())
            val sut: PointPolicyService = PointPolicyService(policyRepository)

            When("허용 범위를 벗어난 1회 적립 한도로 정책을 등록하면") {
                Then("예외가 발생하고 기존 정책은 유지된다") {
                    shouldThrow<PointDomainException> {
                        sut.createOrUpdate(
                            dto =
                                UpsertPointPolicyDto(
                                    maxEarnPerTransaction = BigDecimal(200_000),
                                    maxHoldingAmount = BigDecimal(1_000_000),
                                    defaultExpirationDays = 180L,
                                    appliedAt = LocalDateTime.now(),
                                    createdByAdminId = "admin-01",
                                ),
                        )
                    }
                    policyRepository.policy shouldBe pointPolicy()
                }
            }
        }
    })
