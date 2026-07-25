package com.jysohn0825.point.infrastructure.config

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object RedisTestContainer {
    val instance: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .apply { start() }
    }
}
