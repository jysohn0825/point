package com.jysohn0825.point.domain.lock

import java.time.Duration

// TODO 도메인 레이어에 있는 것은 적합하지 않을 수 있으나, 관련하여 새로운 모듈을 만드는 것은 오버엔지니어링이라고 판단

/** 동일 key에 대한 동시 실행을 직렬화하기 위한 분산락 포트. */
interface DistributedLockExecutor {
    fun <T> executeWithLock(
        key: String,
        waitTime: Duration = Duration.ofSeconds(5),
        leaseTime: Duration = Duration.ofSeconds(3),
        action: () -> T,
    ): T
}

class LockAcquisitionException(
    message: String,
) : RuntimeException(message)
