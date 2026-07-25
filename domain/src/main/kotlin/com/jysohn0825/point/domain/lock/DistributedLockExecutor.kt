package com.jysohn0825.point.domain.lock

import java.time.Duration

/** 동일 key에 대한 동시 실행을 직렬화하기 위한 분산락 포트. */
interface DistributedLockExecutor {
    fun <T> executeWithLock(
        key: String,
        waitTime: Duration = Duration.ofSeconds(5),
        leaseTime: Duration = Duration.ofSeconds(3),
        action: () -> T,
    ): T
}
