package com.jysohn0825.point.infrastructure.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.jysohn0825.point.support.cache.CacheExecutor
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 없이 단일 인스턴스로 배포/실행한다는 전제로, JVM 힙 위의 Caffeine 캐시에 값을 보관하는 구현체.
 * [CacheExecutor]는 호출부가 key마다 다른 TTL을 넘길 수 있는 계약이라, 캐시 전체에 고정 TTL을 거는
 * Caffeine의 기본 `expireAfterWrite` 대신 엔트리별로 TTL을 계산하는 [Expiry]를 직접 구현해 사용한다.
 * 인스턴스가 여러 대로 늘어나면 인스턴스 간 캐시가 공유되지 않으므로, 그 시점에는 이 클래스만
 * Redisson 등 실제 분산 캐시 구현체로 교체하면 된다([CacheExecutor] 포트로 이미 추상화됨).
 */
@Component
class InMemoryCacheExecutor : CacheExecutor {
    private data class CacheEntry(
        val value: Any,
        val ttl: Duration,
    )

    private val cache: Cache<String, CacheEntry> =
        Caffeine
            .newBuilder()
            .expireAfter(
                object : Expiry<String, CacheEntry> {
                    override fun expireAfterCreate(
                        key: String,
                        value: CacheEntry,
                        currentTime: Long,
                    ): Long = value.ttl.toNanos()

                    override fun expireAfterUpdate(
                        key: String,
                        value: CacheEntry,
                        currentTime: Long,
                        currentDuration: Long,
                    ): Long = value.ttl.toNanos()

                    override fun expireAfterRead(
                        key: String,
                        value: CacheEntry,
                        currentTime: Long,
                        currentDuration: Long,
                    ): Long = currentDuration
                },
            ).build()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? = cache.getIfPresent(key)?.value as T?

    override fun <T : Any> put(
        key: String,
        value: T,
        ttl: Duration,
    ) {
        cache.put(key, CacheEntry(value = value, ttl = ttl))
    }

    override fun evict(key: String) {
        cache.invalidate(key)
    }

    override fun <T : Any> getOrPut(
        key: String,
        ttl: Duration,
        loader: () -> T,
    ): T {
        val cached: T? = get(key = key)
        if (cached != null) {
            return cached
        }

        val loaded: T = loader()
        put(key = key, value = loaded, ttl = ttl)
        return loaded
    }
}
