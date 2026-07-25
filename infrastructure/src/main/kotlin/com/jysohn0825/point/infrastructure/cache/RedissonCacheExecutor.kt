package com.jysohn0825.point.infrastructure.cache

import com.jysohn0825.point.support.cache.CacheExecutor
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedissonCacheExecutor(
    private val redissonClient: RedissonClient,
) : CacheExecutor {
    override fun <T : Any> get(key: String): T? {
        val bucket: RBucket<T> = redissonClient.getBucket(key)
        return bucket.get()
    }

    override fun <T : Any> put(
        key: String,
        value: T,
        ttl: Duration,
    ) {
        val bucket: RBucket<T> = redissonClient.getBucket(key)
        bucket.set(value, ttl)
    }

    override fun evict(key: String) {
        redissonClient.getBucket<Any>(key).delete()
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
