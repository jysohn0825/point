package com.jysohn0825.point.support.cache

import java.time.Duration

/** key 기준으로 값을 조회/저장/무효화하는 캐시 포트. */
interface CacheExecutor {
    fun <T : Any> get(key: String): T?

    fun <T : Any> put(
        key: String,
        value: T,
        ttl: Duration,
    )

    fun evict(key: String)

    fun <T : Any> getOrPut(
        key: String,
        ttl: Duration,
        loader: () -> T,
    ): T
}
