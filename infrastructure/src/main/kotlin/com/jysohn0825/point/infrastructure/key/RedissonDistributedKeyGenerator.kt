package com.jysohn0825.point.infrastructure.key

import com.jysohn0825.point.support.key.DistributedKeyGenerator
import org.redisson.api.RIdGenerator
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

private const val INITIAL_VALUE: Long = 1
private const val ALLOCATION_SIZE: Long = 1000

@Component
class RedissonDistributedKeyGenerator(
    private val redissonClient: RedissonClient,
) : DistributedKeyGenerator {
    override fun next(name: String): Long {
        val idGenerator: RIdGenerator = redissonClient.getIdGenerator(name)
        idGenerator.tryInit(INITIAL_VALUE, ALLOCATION_SIZE)
        return idGenerator.nextId()
    }
}
