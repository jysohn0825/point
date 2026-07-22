package com.jysohn0825.point.domain.vo

import java.util.UUID

@JvmInline
value class EarningId(
    val value: UUID,
) {
    companion object {
        fun generate(): EarningId = EarningId(UUID.randomUUID())
    }
}
