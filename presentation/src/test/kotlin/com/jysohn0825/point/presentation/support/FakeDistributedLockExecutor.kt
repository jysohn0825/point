package com.jysohn0825.point.presentation.support

import com.jysohn0825.point.support.lock.DistributedLockExecutor
import java.time.Duration

/** e2e 테스트는 단일 스레드로 순차 호출되므로 실제 락 없이 바로 실행한다. */
class FakeDistributedLockExecutor : DistributedLockExecutor {
    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        action: () -> T,
    ): T = action()
}
