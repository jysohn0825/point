package com.jysohn0825.point.domain.vo

import java.time.LocalDateTime

fun expirationDate(value: LocalDateTime = LocalDateTime.of(2999, 1, 1, 0, 0)): ExpirationDate = ExpirationDate(value)
