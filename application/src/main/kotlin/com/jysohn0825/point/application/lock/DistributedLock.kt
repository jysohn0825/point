package com.jysohn0825.point.application.lock

/**
 * 메서드 호출 전체를 분산락으로 감싼다. key는 메서드 인자를 참조하는 SpEL 표현식이다 (예: "#dto.memberId").
 * waitTimeSeconds/leaseTimeSeconds 기본값(3초/5초)은 짧은 단일 트랜잭션(지갑 갱신 + 적립건 저장)을
 * 보호하기 위한 값으로, leaseTime은 트랜잭션이 끝나기 전에 락이 만료되지 않도록 충분한 여유를 둔다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTimeSeconds: Long = 3,
    val leaseTimeSeconds: Long = 5,
)
