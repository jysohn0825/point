package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.fixture.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CancellationLineTest :
    FunSpec({
        test("복원액이 원 라인 차감액 이하이면 생성할 수 있다") {
            val originalLine = usageLine(amount = 1_000L)

            val cancellationLine = CancellationLine(originalLine, 1_000L, RestorationType.RESTORED)

            cancellationLine.originalLine shouldBe originalLine
            cancellationLine.restoredAmount shouldBe 1_000L
            cancellationLine.restorationType shouldBe RestorationType.RESTORED
        }

        test("만료되어 신규적립으로 복원하는 경우 RE_EARNED로 생성할 수 있다") {
            val originalLine = usageLine(amount = 1_000L)

            val cancellationLine = CancellationLine(originalLine, 300L, RestorationType.RE_EARNED)

            cancellationLine.restorationType shouldBe RestorationType.RE_EARNED
        }

        test("복원액이 0이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                CancellationLine(usageLine(amount = 1_000L), 0L, RestorationType.RESTORED)
            }
        }

        test("복원액이 음수이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                CancellationLine(usageLine(amount = 1_000L), -1L, RestorationType.RESTORED)
            }
        }

        test("복원액이 원 라인 차감액을 초과하면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                CancellationLine(usageLine(amount = 1_000L), 1_001L, RestorationType.RESTORED)
            }
        }

        test("같은 값을 가진 CancellationLine은 서로 동등하다") {
            val originalLine = usageLine(amount = 1_000L)

            CancellationLine(originalLine, 500L, RestorationType.RESTORED) shouldBe
                CancellationLine(originalLine, 500L, RestorationType.RESTORED)
        }

        test("복원방식이 다르면 서로 동등하지 않다") {
            val originalLine = usageLine(amount = 1_000L)

            val restored = CancellationLine(originalLine, 500L, RestorationType.RESTORED)
            val reEarned = CancellationLine(originalLine, 500L, RestorationType.RE_EARNED)

            (restored == reEarned) shouldBe false
        }

        test("copy로 복원액만 변경한 새 인스턴스를 만들 수 있다") {
            val originalLine = usageLine(amount = 1_000L)
            val cancellationLine = CancellationLine(originalLine, 500L, RestorationType.RESTORED)

            val copied = cancellationLine.copy(restoredAmount = 300L)

            copied.originalLine shouldBe originalLine
            copied.restoredAmount shouldBe 300L
        }

        test("toString은 필드 값을 포함한다") {
            val originalLine = usageLine(amount = 1_000L)
            val cancellationLine = CancellationLine(originalLine, 500L, RestorationType.RESTORED)

            cancellationLine.toString() shouldBe
                "CancellationLine(originalLine=$originalLine, restoredAmount=500, restorationType=RESTORED)"
        }

        test("RestorationType의 모든 값을 열거할 수 있다") {
            RestorationType.values().toList() shouldBe listOf(RestorationType.RESTORED, RestorationType.RE_EARNED)
        }

        test("RestorationType은 이름으로 값을 조회할 수 있다") {
            RestorationType.valueOf("RESTORED") shouldBe RestorationType.RESTORED
        }
    })
