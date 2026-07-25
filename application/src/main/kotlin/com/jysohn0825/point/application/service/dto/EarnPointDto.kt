package com.jysohn0825.point.application.service.dto

import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import java.math.BigDecimal

data class EarnPointDto(
    val memberId: String,
    val amount: BigDecimal,
    val earnType: EarnType,
    val sourceReferenceId: String,
    val grantedByAdminId: String? = null,
    /** 미지정 시 정책의 기본 만료 기간을 사용한다. 관리자 수기 지급 시 개별 만료일을 지정하기 위한 값. */
    val expirationPeriod: ExpirationPeriod? = null,
)
