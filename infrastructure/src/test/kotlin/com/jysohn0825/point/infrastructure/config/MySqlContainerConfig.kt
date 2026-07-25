package com.jysohn0825.point.infrastructure.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

object MySqlTestContainer {
    val instance: MySQLContainer<*> by lazy {
        MySQLContainer(DockerImageName.parse("mysql:8.0")).apply { start() }
    }
}

@TestConfiguration(proxyBeanMethods = false)
class MySqlContainerConfig {
    @Bean
    @ServiceConnection
    fun mySQLContainer(): MySQLContainer<*> = MySqlTestContainer.instance
}
