package com.jysohn0825.point.presentation.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * `infrastructure`(JPA/Redis) 없이 컨트롤러~서비스 계층을 e2e로 검증하기 위해,
 * domain repository 포트와 분산락 포트를 fake 구현체로 대체해 등록한다.
 * `@Bean` 반환 타입을 fake 구현 클래스로 그대로 노출해, 테스트에서 `seed`/`clear` 같은
 * fake 전용 메서드를 바로 주입받아 쓸 수 있게 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class PresentationTestConfig {
    @Bean
    fun walletRepository(): FakePointWalletRepository = FakePointWalletRepository()

    @Bean
    fun earningRepository(): FakePointEarningRepository = FakePointEarningRepository()

    @Bean
    fun usageRepository(): FakePointUsageRepository = FakePointUsageRepository()

    @Bean
    fun policyRepository(): FakePointPolicyRepository = FakePointPolicyRepository()

    @Bean
    fun distributedLockExecutor(): FakeDistributedLockExecutor = FakeDistributedLockExecutor()
}
