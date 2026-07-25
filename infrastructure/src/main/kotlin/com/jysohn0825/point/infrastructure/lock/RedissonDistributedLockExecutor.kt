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
        leaseTime: Duration,
        action: () -> T,
    ): T {
        val lock: RLock = redissonClient.getLock(key)
        val acquired: Boolean =
            try {
                lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)
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
