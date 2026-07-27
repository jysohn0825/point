package com.jysohn0825.point.infrastructure.persistence.adapter.mapper

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.pointEarning
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.grantedBy
import com.jysohn0825.point.domain.vo.pointAmount
import com.jysohn0825.point.infrastructure.persistence.entity.PointEarningEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime

class PointEarningMapperTest :
    BehaviorSpec({
        Given("수기지급 적립 엔티티가 있을 때") {
            val entity: PointEarningEntity =
                PointEarningEntity(
                    id = "earning-1",
                    walletId = "wallet-1",
                    policyId = "policy-1",
                    amount = BigDecimal(1_000),
                    remainingAmount = BigDecimal(400),
                    earnType = EarnType.MANUAL.name,
                    sourceReferenceId = "admin-01",
                    grantedByAdminId = "admin-01",
                    earnedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    expiresAt = LocalDateTime.of(2027, 1, 1, 0, 0),
                    status = EarningStatus.ACTIVE.name,
                )

            When("도메인으로 변환하면") {
                val earning: PointEarning = PointEarningMapper.of(entity)

                Then("모든 필드가 그대로 매핑된다") {
                    earning.id shouldBe "earning-1"
                    earning.amount.value shouldBe BigDecimal(1_000L)
                    earning.remainingAmount.value shouldBe BigDecimal(400L)
                    earning.earnType shouldBe EarnType.MANUAL
                    earning.sourceReferenceId shouldBe "admin-01"
                    earning.grantedBy shouldBe "admin-01"
                    earning.earnedAt shouldBe LocalDateTime.of(2026, 1, 1, 0, 0)
                    earning.expirationDate.value shouldBe LocalDateTime.of(2027, 1, 1, 0, 0)
                    earning.status shouldBe EarningStatus.ACTIVE
                }
            }
        }

        Given("일반(SYSTEM) 적립 엔티티가 있을 때") {
            val entity: PointEarningEntity =
                PointEarningEntity(
                    id = "earning-2",
                    walletId = "wallet-1",
                    policyId = "policy-1",
                    amount = BigDecimal(1_000),
                    remainingAmount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM.name,
                    sourceReferenceId = "ORDER-1",
                    grantedByAdminId = null,
                    earnedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    expiresAt = LocalDateTime.of(2027, 1, 1, 0, 0),
                    status = EarningStatus.ACTIVE.name,
                )

            When("도메인으로 변환하면") {
                val earning: PointEarning = PointEarningMapper.of(entity)

                Then("grantedBy는 null이다") {
                    earning.grantedBy.shouldBeNull()
                }
            }
        }

        Given("기존 row가 없는 신규 적립건을 엔티티로 변환할 때") {
            val earning: PointEarning = pointEarning(id = "earning-3", amount = pointAmount(BigDecimal(1_000)))

            When("엔티티로 변환하면") {
                val entity: PointEarningEntity =
                    PointEarningMapper.of(earning = earning, walletId = "wallet-1", policyId = "policy-1", existing = null)

                Then("FK와 금액이 채워지고 canceledAt은 null이다") {
                    entity.walletId shouldBe "wallet-1"
                    entity.policyId shouldBe "policy-1"
                    entity.amount shouldBe BigDecimal(1_000)
                    entity.canceledAt.shouldBeNull()
                }
            }
        }

        Given("ACTIVE였던 적립건이 이번 호출로 CANCELED로 전이할 때") {
            val earning: PointEarning = pointEarning(id = "earning-4", amount = pointAmount(BigDecimal(1_000)))
            earning.cancelEarning()
            val existing: PointEarningEntity =
                PointEarningEntity(
                    id = "earning-4",
                    walletId = "wallet-1",
                    policyId = "policy-1",
                    amount = BigDecimal(1_000),
                    remainingAmount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM.name,
                    sourceReferenceId = "ORDER-1",
                    earnedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plusDays(1),
                    status = EarningStatus.ACTIVE.name,
                    canceledAt = null,
                )

            When("엔티티로 변환하면") {
                val entity: PointEarningEntity =
                    PointEarningMapper.of(earning = earning, walletId = "wallet-1", policyId = "policy-1", existing = existing)

                Then("canceledAt이 지금 시각으로 채워진다") {
                    entity.canceledAt.shouldNotBeNull()
                    entity.status shouldBe EarningStatus.CANCELED.name
                }
            }
        }

        Given("이미 CANCELED 상태였던 적립건을 다시 저장할 때") {
            val earning: PointEarning = pointEarning(id = "earning-5", amount = pointAmount(BigDecimal(1_000)))
            earning.cancelEarning()
            val originalCanceledAt: LocalDateTime = LocalDateTime.of(2026, 3, 1, 12, 0)
            val existing: PointEarningEntity =
                PointEarningEntity(
                    id = "earning-5",
                    walletId = "wallet-1",
                    policyId = "policy-1",
                    amount = BigDecimal(1_000),
                    remainingAmount = BigDecimal.ZERO,
                    earnType = EarnType.SYSTEM.name,
                    sourceReferenceId = "ORDER-1",
                    earnedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plusDays(1),
                    status = EarningStatus.CANCELED.name,
                    canceledAt = originalCanceledAt,
                )

            When("엔티티로 변환하면") {
                val entity: PointEarningEntity =
                    PointEarningMapper.of(earning = earning, walletId = "wallet-1", policyId = "policy-1", existing = existing)

                Then("기존 canceledAt이 그대로 보존된다") {
                    entity.canceledAt shouldBe originalCanceledAt
                }
            }
        }

        Given("수기지급 적립건을 엔티티로 변환할 때") {
            val earning: PointEarning =
                pointEarning(id = "earning-6", amount = pointAmount(BigDecimal(1_000)), earnType = EarnType.MANUAL, grantedBy = grantedBy())

            When("엔티티로 변환하면") {
                val entity: PointEarningEntity =
                    PointEarningMapper.of(earning = earning, walletId = "wallet-1", policyId = "policy-1", existing = null)

                Then("grantedByAdminId가 채워진다") {
                    entity.grantedByAdminId shouldBe grantedBy()
                }
            }
        }
    })
