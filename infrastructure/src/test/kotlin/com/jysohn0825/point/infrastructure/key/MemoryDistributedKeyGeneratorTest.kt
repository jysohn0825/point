package com.jysohn0825.point.infrastructure.key

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MemoryDistributedKeyGeneratorTest :
    BehaviorSpec({
        Given("동일한 name으로 반복해서 채번할 때") {
            val generator: MemoryDistributedKeyGenerator = MemoryDistributedKeyGenerator()
            val name: String = "test-key-sequence"

            When("연속으로 채번하면") {
                val first: Long = generator.next(name = name)
                val second: Long = generator.next(name = name)

                Then("단조 증가하는 서로 다른 키가 반환된다") {
                    second shouldBeGreaterThan first
                }
            }
        }

        Given("서로 다른 name으로 채번할 때") {
            val generator: MemoryDistributedKeyGenerator = MemoryDistributedKeyGenerator()

            When("연속으로 채번하면") {
                val first: Long = generator.next(name = "test-key-a")
                val second: Long = generator.next(name = "test-key-b")

                Then("name과 무관하게 전역적으로 단조 증가하는 키가 반환된다") {
                    second shouldBeGreaterThan first
                }
            }
        }

        Given("짧은 시간 안에 대량으로 반복 채번할 때") {
            val generator: MemoryDistributedKeyGenerator = MemoryDistributedKeyGenerator()
            val name: String = "test-key-burst"
            val issueCount: Int = 10000

            When("연속으로 채번하면") {
                val issued: List<Long> = (1..issueCount).map { generator.next(name = name) }

                Then("중복 없이 단조 증가하는 키가 발급된다") {
                    issued.toSet() shouldHaveSize issueCount
                    issued shouldBe issued.sorted()
                }
            }
        }

        Given("여러 스레드가 동시에 채번할 때") {
            val generator: MemoryDistributedKeyGenerator = MemoryDistributedKeyGenerator()
            val name: String = "test-key-concurrent"
            val threadCount: Int = 50
            val pool: ExecutorService = Executors.newFixedThreadPool(threadCount)
            val latch: CountDownLatch = CountDownLatch(threadCount)
            val issued: AtomicReference<MutableSet<Long>> = AtomicReference(Collections.synchronizedSet(mutableSetOf()))

            When("동시에 채번을 요청하면") {
                repeat(threadCount) {
                    pool.submit {
                        try {
                            issued.get().add(generator.next(name = name))
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await(30, TimeUnit.SECONDS)
                pool.shutdown()

                Then("중복 없이 서로 다른 키가 발급된다") {
                    issued.get() shouldHaveSize threadCount
                }
            }
        }
    })
