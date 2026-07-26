package com.jysohn0825.point.infrastructure.persistence.entity

import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig::class)
class PointWalletEntityContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `MySQL 컨테이너에 적용된 schema-sql 기준으로 지갑 엔티티를 저장하고 조회한다`() {
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
