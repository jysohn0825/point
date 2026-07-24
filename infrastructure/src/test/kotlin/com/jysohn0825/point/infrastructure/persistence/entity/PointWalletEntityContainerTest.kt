package com.jysohn0825.point.infrastructure.persistence.entity

import com.jysohn0825.point.infrastructure.config.MySqlContainerConfig
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MySqlContainerConfig::class)
class PointWalletEntityContainerTest {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `MySQL 컨테이너에 적용된 schema-sql 기준으로 지갑 엔티티를 저장하고 조회한다`() {
        val wallet = PointWalletEntity(memberId = 1L, balance = 1_000L)

        entityManager.persist(wallet)
        entityManager.flush()
        entityManager.clear()

        val found = entityManager.find(PointWalletEntity::class.java, wallet.id)

        found.memberId shouldBe 1L
        found.balance shouldBe 1_000L
    }
}
