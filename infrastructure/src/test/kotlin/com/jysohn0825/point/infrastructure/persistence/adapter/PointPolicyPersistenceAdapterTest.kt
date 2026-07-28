package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.expirationPeriod
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import com.jysohn0825.point.domain.vo.maxHoldingAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime

@DataJpaTest
@Import(PointPolicyPersistenceAdapter::class, FakeCacheExecutor::class)
class PointPolicyPersistenceAdapterTest(
    @Autowired private val entityManager: EntityManager,
    @Autowired private val adapter: PointPolicyPersistenceAdapter,
    @Autowired private val cacheExecutor: FakeCacheExecutor,
) : BehaviorSpec({
        beforeEach {
            cacheExecutor.clear()
        }

        Given("적용 가능한 정책이 없을 때") {
            When("현재 정책을 조회하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> { adapter.getCurrent() }
                }
            }
        }

        Given("정책을 저장할 때") {
            When("정책을 저장하면") {
                Then("버전이 1부터 자동으로 채번된다") {
                    val policy: PointPolicy =
                        pointPolicy(
                            maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(50_000)),
                            maxHoldingAmount = maxHoldingAmount(BigDecimal(1_000_000)),
                            defaultExpirationPeriod = expirationPeriod(365),
                        )

                    adapter.save(policy = policy, appliedAt = LocalDateTime.now().minusDays(1), createdByAdminId = "admin-01")
                    entityManager.flush()
                    entityManager.clear()

                    val current: PointPolicy = adapter.getCurrent()

                    current.maxEarnPerTransaction.value shouldBe BigDecimal(50_000)
                    current.maxHoldingAmount.value shouldBe BigDecimal(1_000_000)
                    current.defaultExpirationPeriod.days shouldBe 365L
                }
            }
        }

        Given("여러 버전의 정책이 저장되어 있을 때") {
            When("현재 정책을 조회하면") {
                Then("적용 시각이 지난 것 중 가장 최신 버전이 조회된다") {
                    adapter.save(
                        policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(500_000))),
                        appliedAt = LocalDateTime.now().minusDays(10),
                        createdByAdminId = "admin-01",
                    )
                    adapter.save(
                        policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(900_000))),
                        appliedAt = LocalDateTime.now().minusDays(1),
                        createdByAdminId = "admin-01",
                    )
                    adapter.save(
                        policy = pointPolicy(maxHoldingAmount = maxHoldingAmount(BigDecimal(1_500_000))),
                        appliedAt = LocalDateTime.now().plusDays(30),
                        createdByAdminId = "admin-01",
                    )
                    entityManager.flush()
                    entityManager.clear()

                    val current: PointPolicy = adapter.getCurrent()

                    current.maxHoldingAmount.value shouldBe BigDecimal(900_000)
                }
            }
        }
    })
