package com.jysohn0825.point.infrastructure.lock

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InMemoryDistributedLockExecutorTest :
    BehaviorSpec({
        Given("동일한 key로 여러 요청이 동시에 들어왔을 때") {
            val executor: InMemoryDistributedLockExecutor = InMemoryDistributedLockExecutor()
            val key: String = "test-lock-key"
            val counter: AtomicInteger = AtomicInteger(0)
            val threadCount: Int = 10
            val pool: ExecutorService = Executors.newFixedThreadPool(threadCount)
            val latch: CountDownLatch = CountDownLatch(threadCount)

            When("락으로 감싸 실행하면") {
                repeat(threadCount) {
                    pool.submit {
                        try {
                            executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5)) {
                                val current: Int = counter.get()
                                Thread.sleep(10)
                                counter.set(current + 1)
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await(30, TimeUnit.SECONDS)
                pool.shutdown()

                Then("직렬화되어 순차적으로 실행된다") {
                    counter.get() shouldBe threadCount
                }
            }
        }

        Given("락을 획득한 동안 예외가 발생했을 때") {
            val executor: InMemoryDistributedLockExecutor = InMemoryDistributedLockExecutor()
            val key: String = "test-lock-key-release-on-error"

            When("작업이 실패해도") {
                runCatching {
                    executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5)) {
                        throw IllegalStateException("작업 중 실패")
                    }
                }

                Then("락이 해제되어 다음 요청이 실행된다") {
                    val result: String =
                        executor.executeWithLock(key = key, waitTime = Duration.ofSeconds(5)) {
                            "released"
                        }

                    result shouldBe "released"
                }
            }
        }
    })
