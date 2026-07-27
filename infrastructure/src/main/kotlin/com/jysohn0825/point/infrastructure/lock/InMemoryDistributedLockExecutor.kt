package com.jysohn0825.point.infrastructure.lock

import com.jysohn0825.point.support.lock.DistributedLockExecutor
import com.jysohn0825.point.support.lock.LockAcquisitionException
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Redis 없이 단일 인스턴스로 배포/실행한다는 전제로, 실제 분산락(Redisson 등) 대신
 * JVM 내 key별 [ReentrantLock]으로 동시 실행을 직렬화하는 구현체.
 * 인스턴스가 여러 대로 늘어나면 이 구현으로는 직렬화를 보장할 수 없으므로,
 * 그 시점에는 이 클래스만 Redisson 등 실제 분산락 구현체로 교체하면 된다([DistributedLockExecutor] 포트로 이미 추상화됨).
 */
@Component
class InMemoryDistributedLockExecutor : DistributedLockExecutor {
    private val locks: ConcurrentHashMap<String, ReentrantLock> = ConcurrentHashMap()

    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        action: () -> T,
    ): T {
        val lock: ReentrantLock = locks.computeIfAbsent(key) { ReentrantLock() }

        val acquired: Boolean =
            try {
                lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw LockAcquisitionException("락 획득 대기 중 인터럽트되었습니다: $key")
            }

        if (!acquired) {
            throw LockAcquisitionException("락 획득에 실패했습니다: $key")
        }

        try {
            return action()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
