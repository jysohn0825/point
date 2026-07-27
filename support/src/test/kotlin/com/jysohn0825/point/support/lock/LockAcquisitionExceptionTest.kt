package com.jysohn0825.point.support.lock

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LockAcquisitionExceptionTest :
    BehaviorSpec({
        Given("락 획득 실패 메시지가 주어지면") {
            When("LockAcquisitionException을 생성하면") {
                val exception: LockAcquisitionException = LockAcquisitionException("lock:member-1 획득에 실패했습니다")

                Then("RuntimeException이며 메시지가 그대로 유지된다") {
                    exception.shouldBeInstanceOf<RuntimeException>()
                    exception.message shouldBe "lock:member-1 획득에 실패했습니다"
                }
            }
        }
    })
