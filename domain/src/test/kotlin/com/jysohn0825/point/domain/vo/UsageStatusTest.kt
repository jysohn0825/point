package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UsageStatusTest : FunSpec({
    test("모든 상태 값을 열거할 수 있다") {
        UsageStatus.values().toList() shouldBe
            listOf(UsageStatus.USED, UsageStatus.PARTIALLY_CANCELED, UsageStatus.FULLY_CANCELED)
    }

    test("이름으로 값을 조회할 수 있다") {
        UsageStatus.valueOf("USED") shouldBe UsageStatus.USED
    }
})
