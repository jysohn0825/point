package com.jysohn0825.point.infrastructure.persistence

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["com.jysohn0825.point.infrastructure.persistence.entity"])
@EnableJpaRepositories(basePackages = ["com.jysohn0825.point.infrastructure.persistence.repository"])
class JpaConfig
