package com.jysohn0825.point.infrastructure.key

import com.jysohn0825.point.support.key.DistributedKeyGenerator
import org.springframework.stereotype.Component

private const val EPOCH_MILLIS: Long = 1704067200000L // 2024-01-01T00:00:00Z
private const val NODE_ID: Long = 0L
private const val SEQUENCE_BITS: Int = 12
private const val NODE_ID_BITS: Int = 10
private const val NODE_ID_SHIFT: Int = SEQUENCE_BITS
private const val TIMESTAMP_SHIFT: Int = SEQUENCE_BITS + NODE_ID_BITS
private const val MAX_SEQUENCE: Long = (1L shl SEQUENCE_BITS) - 1

/**
 * Redis 없이 단일 인스턴스로 배포/실행한다는 전제로, JVM 힙 위에서 타임스탬프(41bit)+노드ID(10bit)+
 * 시퀀스(12bit)를 비트 조합해 채번하는 Snowflake 방식 구현체. 채번값이 현재 시각을 기준으로 만들어지므로,
 * 이전 구현(name별 [java.util.concurrent.atomic.AtomicLong])과 달리 앱을 재시작해도 이전 실행에서 이미
 * 쓰인 id와 겹치지 않는다(시스템 시계가 되돌아가지 않는 한). [DistributedKeyGenerator]의 `name`은 채번
 * 네임스페이스 구분용 파라미터이지만, Snowflake id는 네임스페이스와 무관하게 전역적으로 유일 및 단조
 * 증가하므로 이 구현체에서는 사용하지 않는다.
 *
 * 노드ID는 단일 인스턴스 배포 전제 하에 0으로 고정한다. 인스턴스가 여러 대로 늘어나면 인스턴스마다 서로
 * 다른 노드ID를 갖도록 이 클래스를 교체하거나, 그 시점에는 Redisson 등 실제 분산 채번 구현체로 교체하면
 * 된다([DistributedKeyGenerator] 포트로 이미 추상화됨).
 */
@Component
class MemoryDistributedKeyGenerator : DistributedKeyGenerator {
    private var lastTimestampMillis: Long = -1L
    private var sequence: Long = 0L

    @Synchronized
    override fun next(name: String): Long {
        var currentTimestampMillis: Long = System.currentTimeMillis()

        check(currentTimestampMillis >= lastTimestampMillis) {
            "시스템 시계가 과거로 되돌아가 채번할 수 없다: last=$lastTimestampMillis, current=$currentTimestampMillis"
        }

        if (currentTimestampMillis == lastTimestampMillis) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                currentTimestampMillis = awaitNextMillis(lastTimestampMillis)
            }
        } else {
            sequence = 0L
        }
        lastTimestampMillis = currentTimestampMillis

        return ((currentTimestampMillis - EPOCH_MILLIS) shl TIMESTAMP_SHIFT) or (NODE_ID shl NODE_ID_SHIFT) or sequence
    }

    private fun awaitNextMillis(lastTimestampMillis: Long): Long {
        var timestamp: Long = System.currentTimeMillis()
        while (timestamp <= lastTimestampMillis) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}
