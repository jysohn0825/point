package com.jysohn0825.point.domain.exception

open class PointDomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** 요청한 리소스(지갑/적립건/사용건 등)를 식별자로 찾을 수 없는 경우. `GlobalExceptionHandler`가 HTTP 404로 매핑한다. */
class PointNotFoundException(
    message: String,
) : PointDomainException(message)

/** domain 영역의 생성 시점 불변식 검증용. 위반 시 `IllegalArgumentException` 대신 [PointDomainException]을 던진다. */
fun requireDomain(
    value: Boolean,
    lazyMessage: () -> Any,
) {
    if (!value) {
        throw PointDomainException(lazyMessage().toString())
    }
}

/** domain 영역의 상태 기반 불변식 검증용. 위반 시 `IllegalStateException` 대신 [PointDomainException]을 던진다. */
fun checkDomain(
    value: Boolean,
    lazyMessage: () -> Any,
) {
    if (!value) {
        throw PointDomainException(lazyMessage().toString())
    }
}
