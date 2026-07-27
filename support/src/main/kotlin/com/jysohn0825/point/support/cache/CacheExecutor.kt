package com.jysohn0825.point.support.cache

import java.time.Duration

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
