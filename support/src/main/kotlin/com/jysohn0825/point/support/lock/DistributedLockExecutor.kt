package com.jysohn0825.point.support.lock

import java.time.Duration

/**
 * 동일 key에 대한 동시 실행을 직렬화하기 위한 분산락 포트.
 * 락 점유 시간은 고정 leaseTime이 아니라 구현체가 보유 스레드 생존 여부에 맞춰 자동 연장하는 방식으로 관리한다.
 */
interface DistributedLockExecutor {
    fun <T> executeWithLock(
        key: String,
        waitTime: Duration = Duration.ofSeconds(5),
        action: () -> T,
    ): T
}

class LockAcquisitionException(
    message: String,
) : RuntimeException(message)
