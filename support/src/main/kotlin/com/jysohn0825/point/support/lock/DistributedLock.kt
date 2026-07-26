package com.jysohn0825.point.support.lock

/**
 * 메서드 호출 전체를 분산락으로 감싼다. key는 메서드 인자를 참조하는 SpEL 표현식이다 (예: "#dto.memberId").
 * 락 점유 시간(leaseTime)을 고정값으로 두지 않는다 — 감싸는 트랜잭션 처리 시간이 고정값을 넘기면 트랜잭션이
 * 끝나기 전에 락이 먼저 풀려 동시성 창이 열리기 때문이다. 대신 구현체(Redisson watchdog)가 보유 스레드가
 * 살아있는 동안 자동으로 락을 연장하도록 위임한다.
 *
 * waitTimeSeconds 기본값은 0(대기 없이 즉시 시도)이다. 이 락은 "동일 요청 진짜 동시 처리(in-flight)"를
 * 막기 위한 용도이지, 이미 끝난 요청의 재시도를 흡수하는 멱등성 처리 용도가 아니다 — 멱등성(이미 처리된
 * 요청이면 저장된 결과를 그대로 반환)은 호출부가 락 안에서 기존 데이터를 조회해 별도로 보장한다. 따라서
 * 락 획득에 실패하면 대기해서 앞선 요청이 끝나길 기다리지 않고 즉시 실패해야 진짜 동시 요청을 충돌(409)로
 * 구분할 수 있다. 대기가 필요한 다른 성격의 락이 생기면 그때 호출부에서 waitTimeSeconds를 개별 조정한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTimeSeconds: Long = 0,
)
