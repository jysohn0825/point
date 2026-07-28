package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class CancellationLineTest :
    BehaviorSpec({
        Given("원 라인 차감액 이하의 복원액이 주어지면") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            When("복원 방식이 RESTORED인 CancellationLine을 생성하면") {
                val cancellationLine: CancellationLine =
                    CancellationLine(
                        originalLine = originalLine,
                        restoredAmount = BigDecimal(1_000),
                        restorationType = RestorationType.RESTORED,
                    )

                Then("정상적으로 생성된다") {
                    cancellationLine.originalLine shouldBe originalLine
                    cancellationLine.restoredAmount shouldBe BigDecimal(1_000)
                    cancellationLine.restorationType shouldBe RestorationType.RESTORED
                }
            }

            When("만료되어 신규적립으로 복원하면") {
                val cancellationLine: CancellationLine =
                    CancellationLine(
                        originalLine = originalLine,
                        restoredAmount = BigDecimal(300),
                        restorationType = RestorationType.RE_EARNED,
                    )

                Then("RE_EARNED로 생성된다") {
                    cancellationLine.restorationType shouldBe RestorationType.RE_EARNED
                }
            }
        }

        Given("복원액이 0이면") {
            When("CancellationLine을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        CancellationLine(
                            originalLine = usageLine(amount = BigDecimal(1_000)),
                            restoredAmount = BigDecimal.ZERO,
                            restorationType = RestorationType.RESTORED,
                        )
                    }
                }
            }
        }

        Given("복원액이 음수이면") {
            When("CancellationLine을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        CancellationLine(
                            originalLine = usageLine(amount = BigDecimal(1_000)),
                            restoredAmount = BigDecimal(-1),
                            restorationType = RestorationType.RESTORED,
                        )
                    }
                }
            }
        }

        Given("복원액이 원 라인 차감액을 초과하면") {
            When("CancellationLine을 생성하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        CancellationLine(
                            originalLine = usageLine(amount = BigDecimal(1_000)),
                            restoredAmount = BigDecimal(1_001),
                            restorationType = RestorationType.RESTORED,
                        )
                    }
                }
            }
        }

        Given("같은 값을 가진 두 CancellationLine이 주어지면") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))

            When("동등성을 비교하면") {
                Then("서로 동등하다") {
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
            }
        }

        Given("복원방식만 다른 두 CancellationLine이 주어지면") {
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

            When("동등성을 비교하면") {
                Then("서로 동등하지 않다") {
                    (restored == reEarned) shouldBe false
                }
            }
        }

        Given("CancellationLine이 주어지면") {
            val originalLine: UsageLine = usageLine(amount = BigDecimal(1_000))
            val cancellationLine: CancellationLine =
                CancellationLine(
                    originalLine = originalLine,
                    restoredAmount = BigDecimal(500),
                    restorationType = RestorationType.RESTORED,
                )

            When("copy로 복원액만 변경하면") {
                val copied: CancellationLine = cancellationLine.copy(restoredAmount = BigDecimal(300))

                Then("나머지 값은 유지된 새 인스턴스가 만들어진다") {
                    copied.originalLine shouldBe originalLine
                    copied.restoredAmount shouldBe BigDecimal(300)
                }
            }
        }

        Given("RestorationType이 주어지면") {
            When("모든 값을 열거하면") {
                Then("RESTORED와 RE_EARNED 두 값이 반환된다") {
                    RestorationType.values().toList() shouldBe listOf(RestorationType.RESTORED, RestorationType.RE_EARNED)
                }
            }

            When("이름으로 값을 조회하면") {
                Then("해당하는 값이 반환된다") {
                    RestorationType.valueOf("RESTORED") shouldBe RestorationType.RESTORED
                }
            }
        }
    })
