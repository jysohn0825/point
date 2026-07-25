package com.jysohn0825.point.infrastructure.persistence.adapter

import com.jysohn0825.point.support.cache.CacheExecutor
import java.time.Duration

class FakeCacheExecutor : CacheExecutor {
    private val store: MutableMap<String, Any> = mutableMapOf()

    fun clear() {
        store.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? = store[key] as T?

    override fun <T : Any> put(
        key: String,
        value: T,
        ttl: Duration,
    ) {
        store[key] = value
    }

    override fun evict(key: String) {
        store.remove(key)
    }

    override fun <T : Any> getOrPut(
        key: String,
        ttl: Duration,
        loader: () -> T,
    ): T {
        val cached: T? = get(key = key)
        if (cached != null) return cached
        val loaded: T = loader()
        put(key = key, value = loaded, ttl = ttl)
        return loaded
    }
}
