package com.jysohn0825.point.domain.vo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID

class EarningIdTest : FunSpec({
    test("generate()는 매번 새로운 EarningId를 생성한다") {
        val first = EarningId.generate()
        val second = EarningId.generate()

        first shouldNotBe second
    }

    test("동일한 UUID를 가진 EarningId는 서로 동등하다") {
        val uuid = UUID.randomUUID()

        (EarningId(uuid) == EarningId(uuid)) shouldBe true
    }

    test("다른 UUID를 가진 EarningId는 서로 동등하지 않다") {
        (EarningId(UUID.randomUUID()) == EarningId(UUID.randomUUID())) shouldBe false
    }

    test("value 프로퍼티로 원본 UUID에 접근할 수 있다") {
        val uuid = UUID.randomUUID()

        EarningId(uuid).value shouldBe uuid
    }

    test("toString은 UUID 값을 포함한다") {
        val uuid = UUID.randomUUID()

        EarningId(uuid).toString() shouldContain uuid.toString()
    }

    test("hashCode는 동일한 UUID에 대해 동일하다") {
        val uuid = UUID.randomUUID()

        EarningId(uuid).hashCode() shouldBe EarningId(uuid).hashCode()
    }
})
