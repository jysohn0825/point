package com.jysohn0825.point.presentation.support

import com.jysohn0825.point.support.key.DistributedKeyGenerator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** e2e 테스트는 실제 분산 채번 없이 name별로 순차 증가하는 값을 내준다. */
class FakeDistributedKeyGenerator : DistributedKeyGenerator {
    private val counters: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap()

    override fun next(name: String): Long = counters.computeIfAbsent(name) { AtomicLong() }.incrementAndGet()
}
