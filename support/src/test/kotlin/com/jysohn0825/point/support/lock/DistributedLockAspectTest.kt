package com.jysohn0825.point.support.lock

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Component
import java.time.Duration

data class LockTarget(
    val id: String,
)

@Component
class SampleLockedService {
    @DistributedLock(key = "'lock:' + #target.id")
    fun run(target: LockTarget): String = "executed-${target.id}"
}

private class RecordingLockExecutor : DistributedLockExecutor {
    val capturedKeys: MutableList<String> = mutableListOf()

    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        action: () -> T,
    ): T {
        capturedKeys.add(key)
        return action()
    }
}

@Configuration
@EnableAspectJAutoProxy
class DistributedLockTestConfig {
    @Bean
    fun lockExecutor(): DistributedLockExecutor = RecordingLockExecutor()

    @Bean
    fun distributedLockAspect(lockExecutor: DistributedLockExecutor): DistributedLockAspect = DistributedLockAspect(lockExecutor)

    @Bean
    fun sampleLockedService(): SampleLockedService = SampleLockedService()
}

class DistributedLockAspectTest :
    FunSpec({
        test("메서드 인자를 참조한 SpEL key가 그대로 락 실행기에 전달되고, 원래 메서드 결과가 반환된다") {
            val context: AnnotationConfigApplicationContext = AnnotationConfigApplicationContext(DistributedLockTestConfig::class.java)

            val service: SampleLockedService = context.getBean(SampleLockedService::class.java)
            val executor: RecordingLockExecutor = context.getBean(DistributedLockExecutor::class.java) as RecordingLockExecutor

            val result: String = service.run(LockTarget("abc"))

            result shouldBe "executed-abc"
            executor.capturedKeys shouldBe listOf("lock:abc")

            context.close()
        }
    })
