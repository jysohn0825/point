package com.jysohn0825.point.domain.exception

open class PointDomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

// TODO require / check / requireNotNull 등 domain 영역에서 체크 되는 영역에 대해 PointDomainException 발생하게 수정
