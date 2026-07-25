package com.jysohn0825.point.infrastructure.cache

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedissonCacheExecutorContainerTest {
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

    private val executor: RedissonCacheExecutor = RedissonCacheExecutor(redissonClient)

    @AfterAll
    fun tearDown() {
        redissonClient.shutdown()
        redisContainer.stop()
    }

    @Test
    fun `put한 값을 get으로 조회할 수 있고, evict 후에는 조회되지 않는다`() {
        val key: String = "test-cache-key"

        executor.put(key = key, value = "cached-value", ttl = Duration.ofMinutes(1))
        executor.get<String>(key = key) shouldBe "cached-value"

        executor.evict(key = key)
        executor.get<String>(key = key) shouldBe null
    }

    @Test
    fun `getOrPut은 캐시가 비어있을 때만 loader를 실행하고 결과를 캐시에 저장한다`() {
        val key: String = "test-cache-getOrPut-key"
        val loadCount: AtomicInteger = AtomicInteger(0)

        val loader = {
            loadCount.incrementAndGet()
            "loaded-value"
        }

        val first: String = executor.getOrPut(key = key, ttl = Duration.ofMinutes(1), loader = loader)
        val second: String = executor.getOrPut(key = key, ttl = Duration.ofMinutes(1), loader = loader)

        first shouldBe "loaded-value"
        second shouldBe "loaded-value"
        loadCount.get() shouldBe 1
    }

    @Test
    fun `TTL이 지나면 값이 만료되어 더 이상 조회되지 않는다`() {
        val key: String = "test-cache-ttl-key"

        executor.put(key = key, value = "expiring-value", ttl = Duration.ofMillis(200))
        executor.get<String>(key = key) shouldNotBe null

        Thread.sleep(500)

        executor.get<String>(key = key) shouldBe null
    }
}
