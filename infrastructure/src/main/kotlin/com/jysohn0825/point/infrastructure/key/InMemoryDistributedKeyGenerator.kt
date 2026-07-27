package com.jysohn0825.point.infrastructure.key

import com.jysohn0825.point.support.key.DistributedKeyGenerator
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private const val INITIAL_VALUE: Long = 0

/**
 * Redis 없이 단일 인스턴스로 배포/실행한다는 전제로, JVM 힙 위의 [AtomicLong]으로 name별 단조 증가
 * 시퀀스를 채번하는 구현체. 인스턴스가 여러 대로 늘어나면 인스턴스 간 채번값이 겹칠 수 있으므로,
 * 그 시점에는 이 클래스만 Redisson 등 실제 분산 채번 구현체로 교체하면 된다([DistributedKeyGenerator] 포트로 이미 추상화됨).
 */
@Component
class InMemoryDistributedKeyGenerator : DistributedKeyGenerator {
    private val sequences: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap()

    override fun next(name: String): Long = sequences.computeIfAbsent(name) { AtomicLong(INITIAL_VALUE) }.incrementAndGet()
}
