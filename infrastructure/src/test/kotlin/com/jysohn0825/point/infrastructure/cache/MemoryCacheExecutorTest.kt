package com.jysohn0825.point.infrastructure.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class MemoryCacheExecutorTest :
    BehaviorSpec({
        Given("값을 캐시에 저장했을 때") {
            val executor: MemoryCacheExecutor = MemoryCacheExecutor()
            val key: String = "test-cache-key"
            executor.put(key = key, value = "cached-value", ttl = Duration.ofMinutes(1))

            When("get으로 조회하면") {
                Then("저장한 값이 조회된다") {
                    executor.get<String>(key = key) shouldBe "cached-value"
                }
            }

            When("evict 후 조회하면") {
                executor.evict(key = key)

                Then("더 이상 조회되지 않는다") {
                    executor.get<String>(key = key) shouldBe null
                }
            }
        }

        Given("캐시가 비어있을 때") {
            val executor: MemoryCacheExecutor = MemoryCacheExecutor()
            val key: String = "test-cache-getOrPut-key"
            val loadCount: AtomicInteger = AtomicInteger(0)
            val loader: () -> String = {
                loadCount.incrementAndGet()
                "loaded-value"
            }

            When("getOrPut을 두 번 호출하면") {
                val first: String = executor.getOrPut(key = key, ttl = Duration.ofMinutes(1), loader = loader)
                val second: String = executor.getOrPut(key = key, ttl = Duration.ofMinutes(1), loader = loader)

                Then("loader는 최초 한 번만 실행되고 캐시된 값이 반환된다") {
                    first shouldBe "loaded-value"
                    second shouldBe "loaded-value"
                    loadCount.get() shouldBe 1
                }
            }
        }

        Given("TTL이 짧게 설정된 값을 저장했을 때") {
            val executor: MemoryCacheExecutor = MemoryCacheExecutor()
            val key: String = "test-cache-ttl-key"
            executor.put(key = key, value = "expiring-value", ttl = Duration.ofMillis(200))

            When("TTL이 지나기 전에 조회하면") {
                Then("값이 조회된다") {
                    executor.get<String>(key = key) shouldNotBe null
                }
            }

            When("TTL이 지난 후 조회하면") {
                Thread.sleep(500)

                Then("값이 만료되어 조회되지 않는다") {
                    executor.get<String>(key = key) shouldBe null
                }
            }
        }
    })
