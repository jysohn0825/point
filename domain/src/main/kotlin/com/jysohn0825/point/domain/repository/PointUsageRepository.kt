package com.jysohn0825.point.domain.repository

import com.jysohn0825.point.domain.entity.PointUsage
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.EarningUsageTrace
import java.time.LocalDateTime

interface PointUsageRepository {
    /** walletId는 PointUsage가 들고 있지 않은 값(순수 FK)이라 저장 시점에 별도로 전달한다. */
    fun save(
        usage: PointUsage,
        walletId: String,
    )

    /**
     * cancel() 호출 직후, 그 결과(usage)와 이번 호출에서 방금 추가된 취소 라인들(requestedLines)을 함께 저장한다.
     * PointUsage.cancellationLines는 과거 모든 취소를 합친 flat list라 "이번 호출분"만 구분할 수 없으므로,
     * point_usage_cancellation 헤더 1건으로 묶기 위해 requestedLines를 별도로 받는다.
     * reearnedEarningIds는 requestedLines 중 RE_EARNED인 라인들만, 등장 순서대로 그 신규 적립건의 id를 담는다
     * (RESTORED 라인은 신규 적립을 만들지 않으므로 이 목록에 나타나지 않는다).
     */
    fun saveCancellation(
        usage: PointUsage,
        walletId: String,
        requestedLines: List<CancellationLine>,
        reearnedEarningIds: List<String> = emptyList(),
        canceledAt: LocalDateTime = LocalDateTime.now(),
    )

    fun findById(usageId: String): PointUsage

    /** 적립건이 어느 주문에서 얼마나 사용됐는지 역추적한다. */
    fun findLinesByEarningId(earningId: String): List<EarningUsageTrace>

    /** 조회 API용 — 지갑의 모든 사용건을 최신순으로 반환한다. */
    fun findAllByWalletId(walletId: String): List<PointUsage>
}
