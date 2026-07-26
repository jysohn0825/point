package com.jysohn0825.point.support.lock

/**
 * 메서드 호출 전체를 분산락으로 감싼다. key는 메서드 인자를 참조하는 SpEL 표현식이다 (예: "#dto.memberId").
 * 락 점유 시간(leaseTime)을 고정값으로 두지 않는다 — 감싸는 트랜잭션 처리 시간이 고정값을 넘기면 트랜잭션이
 * 끝나기 전에 락이 먼저 풀려 동시성 창이 열리기 때문이다. 대신 구현체(Redisson watchdog)가 보유 스레드가
 * 살아있는 동안 자동으로 락을 연장하도록 위임한다. waitTimeSeconds(기본 3초)만 호출자가 조정한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTimeSeconds: Long = 3,
)
