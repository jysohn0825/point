package com.jysohn0825.point.infrastructure.persistence.entity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.math.BigDecimal
import java.util.UUID

@DataJpaTest
class PointWalletEntityTest(
    @Autowired private val entityManager: EntityManager,
) : BehaviorSpec({
        Given("H2에 적용된 schema.sql 기준의 지갑 엔티티가 있을 때") {
            When("엔티티를 저장하면") {
                Then("저장한 값 그대로 조회된다") {
                    val wallet: PointWalletEntity =
                        PointWalletEntity(
                            id = UUID.randomUUID().toString(),
                            memberId = UUID.randomUUID().toString(),
                            balance = BigDecimal(1_000),
                        )

                    entityManager.persist(wallet)
                    entityManager.flush()
                    entityManager.clear()

                    val found: PointWalletEntity = entityManager.find(PointWalletEntity::class.java, wallet.id)

                    found.memberId shouldBe wallet.memberId
                    found.balance shouldBe BigDecimal(1_000)
                }
            }
        }
    })
