package com.jysohn0825.point.infrastructure.lock

import com.jysohn0825.point.support.lock.DistributedLockExecutor
import com.jysohn0825.point.support.lock.LockAcquisitionException
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RedissonDistributedLockExecutor(
    private val redissonClient: RedissonClient,
) : DistributedLockExecutor {
    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        action: () -> T,
    ): T {
        val lock: RLock = redissonClient.getLock(key)
        // leaseTime을 지정하지 않으면 Redisson의 락 watchdog이 보유 스레드가 살아있는 동안
        // (기본 30초 주기로) 자동 연장한다. 감싸는 트랜잭션이 얼마나 걸릴지 고정값으로 예단하지 않아도
        // 트랜잭션 도중 락이 먼저 풀리는 문제(동시성 창 노출)가 생기지 않는다.
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
