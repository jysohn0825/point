package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.fixture.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class CancellationLineTest :
    FunSpec({
        test("복원액이 원 라인 차감액 이하이면 생성할 수 있다") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            val cancellationLine: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(1_000),
                    restorationType = RestorationType.RESTORED,
                )

            cancellationLine.originalLine shouldBe originalLine
            cancellationLine.restoredAmount shouldBe BigDecimal(1_000)
            cancellationLine.restorationType shouldBe RestorationType.RESTORED
        }

        test("만료되어 신규적립으로 복원하는 경우 RE_EARNED로 생성할 수 있다") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            val cancellationLine: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(300),
                    restorationType = RestorationType.RE_EARNED,
                )

            cancellationLine.restorationType shouldBe RestorationType.RE_EARNED
        }

        test("복원액이 0이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                CancellationLine(
                    originalLine = usageLine(amount = BigDecimal(1_000)),
                    restoredAmount = BigDecimal.ZERO,
                    restorationType = RestorationType.RESTORED,
                )
            }
        }

        test("복원액이 음수이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                CancellationLine(
                    originalLine = usageLine(amount = BigDecimal(1_000)),
                    restoredAmount = BigDecimal(-1),
                    restorationType = RestorationType.RESTORED,
                )
            }
        }

        test("복원액이 원 라인 차감액을 초과하면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                CancellationLine(
                    originalLine = usageLine(amount = BigDecimal(1_000)),
                    restoredAmount = BigDecimal(1_001),
                    restorationType = RestorationType.RESTORED,
                )
            }
        }

        test("같은 값을 가진 CancellationLine은 서로 동등하다") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            CancellationLine(
                originalLine = originalLine,
                restoredAmount = BigDecimal(500),
                restorationType = RestorationType.RESTORED,
            ) shouldBe
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(500),
                    restorationType = RestorationType.RESTORED,
                )
        }

        test("복원방식이 다르면 서로 동등하지 않다") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            val restored: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(500),
                    restorationType = RestorationType.RESTORED,
                )
            val reEarned: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(500),
                    restorationType = RestorationType.RE_EARNED,
                )

            (restored == reEarned) shouldBe false
        }

        test("copy로 복원액만 변경한 새 인스턴스를 만들 수 있다") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))
            val cancellationLine: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(500),
                    restorationType = RestorationType.RESTORED,
                )

            val copied: CancellationLine = cancellationLine.copy(restoredAmount = BigDecimal(300))

            copied.originalLine shouldBe originalLine
            copied.restoredAmount shouldBe BigDecimal(300)
        }

        test("RestorationType의 모든 값을 열거할 수 있다") {
            RestorationType.values().toList() shouldBe listOf(RestorationType.RESTORED, RestorationType.RE_EARNED)
        }

        test("RestorationType은 이름으로 값을 조회할 수 있다") {
            RestorationType.valueOf("RESTORED") shouldBe RestorationType.RESTORED
        }
    })
