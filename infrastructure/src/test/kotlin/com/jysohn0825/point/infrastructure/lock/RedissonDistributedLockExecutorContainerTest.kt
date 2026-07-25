package com.jysohn0825.point.infrastructure.lock

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.redisson.Redisson
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedissonDistributedLockExecutorContainerTest {
    private val redisContainer =
        GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .also { it.start() }

    private val redissonClient =
        Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}"
            },
        )

    private val executor = RedissonDistributedLockExecutor(redissonClient)

    @AfterAll
    fun tearDown() {
        redissonClient.shutdown()
        redisContainer.stop()
    }

    @Test
    fun `동일한 key로 동시에 들어온 요청은 직렬화되어 순차적으로 실행된다`() {
        val key = "test-lock-key"
        val counter = AtomicInteger(0)
        val threadCount = 10
        val pool = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                try {
                    executor.executeWithLock(key, Duration.ofSeconds(5), Duration.ofSeconds(5)) {
                        val current = counter.get()
                        Thread.sleep(10)
                        counter.set(current + 1)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        pool.shutdown()

        counter.get() shouldBe threadCount
    }

    @Test
    fun `락을 획득한 동안 예외가 발생해도 락이 해제되어 다음 요청이 실행된다`() {
        val key = "test-lock-key-release-on-error"

        runCatching {
            executor.executeWithLock(key, Duration.ofSeconds(5), Duration.ofSeconds(5)) {
                throw IllegalStateException("작업 중 실패")
            }
        }

        val result =
            executor.executeWithLock(key, Duration.ofSeconds(5), Duration.ofSeconds(5)) {
                "released"
            }

        result shouldBe "released"
    }
}
