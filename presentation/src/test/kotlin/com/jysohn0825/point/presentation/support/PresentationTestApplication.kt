package com.jysohn0825.point.presentation.support

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration

/**
 * presentation e2e 테스트 전용 부트 클래스.
 * 실제 [com.jysohn0825.point.presentation.PointApiApplication]과 달리 `infrastructure`(JPA/Redis)를
 * 스캔 범위에서 제외하고, `presentation`+`application`만 스캔한다. repository 포트는
 * [PresentationTestConfig]가 제공하는 fake 구현체로 대체된다(다른 모듈과 동일하게 mock이 아닌 fake 사용).
 *
 * `infrastructure`가 `runtimeOnly`라 컴파일 의존은 없지만, 실행 시 클래스패스에는 여전히 존재한다.
 * Spring Boot의 자동설정(JPA/Redisson)은 스캔 범위와 무관하게 "클래스패스에 있으면" 동작하므로,
 * scanBasePackages만으로는 실제 DB/Redis 연결을 막을 수 없어 명시적으로 제외해야 한다.
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.jysohn0825.point.presentation.controller",
        "com.jysohn0825.point.presentation.exception",
        "com.jysohn0825.point.presentation.scheduler",
        "com.jysohn0825.point.application",
    ],
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
    ],
    excludeName = [
        // redisson-spring-boot-starter는 infrastructure의 implementation 의존이라 presentation의
        // 컴파일 클래스패스에는 없다(런타임에만 존재) — 클래스 참조 대신 FQN 문자열로 제외한다.
        "org.redisson.spring.starter.RedissonAutoConfigurationV2",
    ],
)
class PresentationTestApplication
