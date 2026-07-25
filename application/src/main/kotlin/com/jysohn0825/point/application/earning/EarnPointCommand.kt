package com.jysohn0825.point.application.earning

import com.jysohn0825.point.domain.vo.EarnType
import java.math.BigDecimal

data class EarnPointCommand(
    val memberId: String,
    val amount: BigDecimal,
    val earnType: EarnType,
    val sourceReferenceId: String,
    val grantedByAdminId: String? = null,
)
