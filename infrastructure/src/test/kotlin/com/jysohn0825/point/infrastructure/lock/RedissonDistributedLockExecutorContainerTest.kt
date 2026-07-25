package com.jysohn0825.point.infrastructure.lock

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedissonDistributedLockExecutorContainerTest {
    private val redissonClient: RedissonClient =
        Redisson.create(
            Config().apply {
                useSingleServer().address = "redis://localhost:6379"
            },
        )

    private val executor: RedissonDistributedLockExecutor = RedissonDistributedLockExecutor(redissonClient)

    @AfterAll
    fun tearDown() {
        redissonClient.shutdown()
    }

    @Test
    fun `동일한 key로 동시에 들어온 요청은 직렬화되어 순차적으로 실행된다`() {
        val key: String = "test-lock-key"
        val counter: AtomicInteger = AtomicInteger(0)
        val threadCount: Int = 10
        val pool: ExecutorService = Executors.newFixedThreadPool(threadCount)
        val latch: CountDownLatch = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                try {
                    executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5), leaseTime = Duration.ofSeconds(5)) {
                        val current: Int = counter.get()
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
        val key: String = "test-lock-key-release-on-error"

        runCatching {
            executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5), leaseTime = Duration.ofSeconds(5)) {
                throw IllegalStateException("작업 중 실패")
            }
        }

        val result: String =
            executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5), leaseTime = Duration.ofSeconds(5)) {
                "released"
            }

        result shouldBe "released"
    }
}
