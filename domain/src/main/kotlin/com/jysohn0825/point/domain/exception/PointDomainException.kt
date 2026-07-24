package com.jysohn0825.point.domain.exception

open class PointDomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
