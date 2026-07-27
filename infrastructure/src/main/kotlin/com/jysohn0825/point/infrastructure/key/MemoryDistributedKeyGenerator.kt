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
 *
 * 알려진 한계: 채번 상태가 JVM 힙에만 있어 앱 재시작 시 name별로 0부터 다시 시작한다. H2가 파일 기반이라
 * 기존 row는 재시작 후에도 남아있으므로, 재시작 직후 채번된 id가 이전 실행에서 이미 쓰인 id와 겹칠 수 있다.
 * 근본적으로는 타임스탬프+노드+시퀀스를 조합하는 Snowflake류 ID로 전환하는 방향을 고려했으나
 * 이번 구현 범위에서는 적용하지 않았다.
 */
@Component
class MemoryDistributedKeyGenerator : DistributedKeyGenerator {
    private val sequences: ConcurrentHashMap<String, AtomicLong> = ConcurrentHashMap()

    override fun next(name: String): Long = sequences.computeIfAbsent(name) { AtomicLong(INITIAL_VALUE) }.incrementAndGet()
}
