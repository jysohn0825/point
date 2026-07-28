package com.jysohn0825.point.application.config

import com.jysohn0825.point.domain.service.DefaultPointCancellationAllocator
import com.jysohn0825.point.domain.service.DefaultPointExpirationAllocator
import com.jysohn0825.point.domain.service.DefaultPointRedemptionAllocator
import com.jysohn0825.point.domain.service.PointCancellationAllocator
import com.jysohn0825.point.domain.service.PointExpirationAllocator
import com.jysohn0825.point.domain.service.PointRedemptionAllocator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AllocatorConfig {
    @Bean
    fun pointRedemptionAllocator(): PointRedemptionAllocator = DefaultPointRedemptionAllocator()

    @Bean
    fun pointCancellationAllocator(): PointCancellationAllocator = DefaultPointCancellationAllocator()

    @Bean
    fun pointExpirationAllocator(): PointExpirationAllocator = DefaultPointExpirationAllocator()
}
