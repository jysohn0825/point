package com.jysohn0825.point.domain.fixture

import com.jysohn0825.point.domain.vo.EarningId
import java.util.UUID

fun earningId(value: UUID = UUID.randomUUID()): EarningId = EarningId(value)
