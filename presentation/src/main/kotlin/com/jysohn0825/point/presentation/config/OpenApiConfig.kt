package com.jysohn0825.point.presentation.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun pointOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Point API")
                    .description("포인트 적립/적립취소/사용/사용취소 API")
                    .version("v1"),
            )
}
