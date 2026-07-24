package com.jysohn0825.point.application.exception

open class PointBusinessException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
