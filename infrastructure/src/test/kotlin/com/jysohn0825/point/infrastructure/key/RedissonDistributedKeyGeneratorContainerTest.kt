package com.jysohn0825.point.infrastructure.key

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedissonDistributedKeyGeneratorContainerTest {
    private val redisContainer: GenericContainer<*> =
        GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .also { it.start() }

    private val redissonClient: RedissonClient =
        Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}"
            },
        )

    private val generator: RedissonDistributedKeyGenerator = RedissonDistributedKeyGenerator(redissonClient)

    @AfterAll
    fun tearDown() {
        redissonClient.shutdown()
        redisContainer.stop()
    }

    @Test
    fun `동일한 name으로 채번하면 단조 증가하는 서로 다른 키가 반환된다`() {
        val name: String = "test-key-sequence"

        val first: Long = generator.next(name = name)
        val second: Long = generator.next(name = name)

        second shouldBe first + 1
    }

    @Test
    fun `여러 스레드가 동시에 채번해도 중복 없이 서로 다른 키가 발급된다`() {
        val name: String = "test-key-concurrent"
        val threadCount: Int = 50
        val pool: ExecutorService = Executors.newFixedThreadPool(threadCount)
        val latch: CountDownLatch = CountDownLatch(threadCount)
        val issued: AtomicReference<MutableSet<Long>> = AtomicReference(java.util.Collections.synchronizedSet(mutableSetOf()))

        repeat(threadCount) {
            pool.submit {
                try {
                    issued.get().add(generator.next(name = name))
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        pool.shutdown()

        issued.get() shouldHaveSize threadCount
    }
}
