package com.jysohn0825.point.infrastructure.key

import com.jysohn0825.point.support.key.DistributedKeyGenerator
import org.redisson.api.RIdGenerator
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private const val INITIAL_VALUE: Long = 1
private const val ALLOCATION_SIZE: Long = 1000

@Component
class RedissonDistributedKeyGenerator(
    private val redissonClient: RedissonClient,
) : DistributedKeyGenerator {
    // RIdGenerator는 로컬 인스턴스에 할당 블록(ALLOCATION_SIZE)을 캐싱해 순차 ID를 내주는 구조라,
    // name마다 인스턴스를 재사용해야 한다. 매번 새로 만들면 호출할 때마다 새 블록을 할당받아 ID가 튄다.
    private val idGenerators: ConcurrentHashMap<String, RIdGenerator> = ConcurrentHashMap()

    override fun next(name: String): Long {
        val idGenerator: RIdGenerator =
            idGenerators.computeIfAbsent(name) { key: String ->
                redissonClient.getIdGenerator(key).apply { tryInit(INITIAL_VALUE, ALLOCATION_SIZE) }
            }
        return idGenerator.nextId()
    }
}
