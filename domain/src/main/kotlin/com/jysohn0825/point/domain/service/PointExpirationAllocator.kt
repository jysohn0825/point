package com.jysohn0825.point.domain.service

import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.vo.PointAmount

/** 만료 대상 적립건들을 어떻게 소멸시키고 지갑에서 뺄 금액을 얼마로 산정할지 결정하는 정책 포트. */
interface PointExpirationAllocator {
    fun allocate(dueEarnings: List<PointEarning>): PointAmount
}
