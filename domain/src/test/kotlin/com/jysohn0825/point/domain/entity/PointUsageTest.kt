package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.fixture.cancellationLine
import com.jysohn0825.point.domain.fixture.earningId
import com.jysohn0825.point.domain.fixture.orderNumber
import com.jysohn0825.point.domain.fixture.pointUsage
import com.jysohn0825.point.domain.fixture.usageLine
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.RestorationType
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PointUsageTest :
    BehaviorSpec({
        given("PointUsage.use()로 사용 건을 생성할 때") {
            `when`("유효한 사용 라인들이 주어지면") {
                then("상태는 USED이고 총액은 라인 합계다") {
                    val lines = listOf(usageLine(amount = 1_000L), usageLine(amount = 2_000L))

                    val usage = PointUsage.use(orderNumber(), lines)

                    usage.lines shouldBe lines
                    usage.totalAmount shouldBe 3_000L
                    usage.remainingAmount shouldBe 3_000L
                    usage.cancelledAmount shouldBe 0L
                    usage.status shouldBe UsageStatus.USED
                    usage.cancellationLines shouldBe emptyList()
                }
            }

            `when`("사용 라인이 비어있으면") {
                then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        PointUsage.use(orderNumber(), emptyList())
                    }
                }
            }

            `when`("동일한 적립건을 참조하는 사용 라인이 여러 개 주어지면") {
                then("모두 허용된다") {
                    val earningId = earningId()
                    val lines =
                        listOf(
                            UsageLine(earningId, 1_000L),
                            UsageLine(earningId, 2_000L),
                        )

                    val usage = PointUsage.use(orderNumber(), lines)

                    usage.lines shouldBe lines
                    usage.totalAmount shouldBe 3_000L
                }
            }
        }

        given("생성된 PointUsage를 취소(cancel)할 때") {
            `when`("일부 금액만 취소하면") {
                then("상태는 PARTIALLY_CANCELED이고 잔여액이 줄어든다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 400L)))

                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                    usage.cancelledAmount shouldBe 400L
                    usage.remainingAmount shouldBe 600L
                    usage.cancellationLines.size shouldBe 1
                }
            }

            `when`("전액을 취소하면") {
                then("상태는 FULLY_CANCELED이고 잔여액은 0이다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 1_000L)))

                    usage.status shouldBe UsageStatus.FULLY_CANCELED
                    usage.remainingAmount shouldBe 0L
                }
            }

            `when`("여러 라인으로 구성된 사용 건을 부분 취소한 뒤 나머지를 마저 취소하면") {
                then("전액 취소가 된다") {
                    val lineA = usageLine(amount = 1_000L)
                    val lineB = usageLine(amount = 2_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(lineA, lineB))

                    usage.cancel(listOf(cancellationLine(originalLine = lineA, restoredAmount = 1_000L)))
                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED

                    usage.cancel(listOf(cancellationLine(originalLine = lineB, restoredAmount = 2_000L)))
                    usage.status shouldBe UsageStatus.FULLY_CANCELED
                    usage.cancellationLines.size shouldBe 2
                }
            }

            `when`("같은 라인을 여러 번 나누어 취소하면") {
                then("취소 금액이 누적된다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 300L)))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 300L)))

                    usage.cancelledAmount shouldBe 600L
                    usage.remainingAmount shouldBe 400L
                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                }
            }

            `when`("취소 요청 라인이 비어있으면") {
                then("예외가 발생한다") {
                    val usage = pointUsage()

                    shouldThrow<IllegalArgumentException> { usage.cancel(emptyList()) }
                }
            }

            `when`("이미 전액 취소된 사용 건을 다시 취소하려 하면") {
                then("예외가 발생한다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 1_000L)))

                    shouldThrow<IllegalArgumentException> {
                        usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 1L)))
                    }
                }
            }

            `when`("이 사용 건에 속하지 않은 라인을 취소하려 하면") {
                then("예외가 발생한다") {
                    val usage = pointUsage(lines = listOf(usageLine(amount = 1_000L)))
                    val foreignLine = usageLine(amount = 500L)

                    shouldThrow<IllegalArgumentException> {
                        usage.cancel(listOf(CancellationLine(foreignLine, 100L, RestorationType.RESTORED)))
                    }
                }
            }

            `when`("누적 취소 요청 금액이 해당 라인의 사용 금액을 초과하면") {
                then("예외가 발생하고 이전 취소 내역은 유지된다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 700L)))

                    shouldThrow<IllegalArgumentException> {
                        usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 400L)))
                    }

                    usage.cancelledAmount shouldBe 700L
                }
            }

            `when`("취소가 반영된 뒤 cancellationLines를 조회하면") {
                then("내부 상태를 보호하는 방어적 복사본을 반환한다") {
                    val line = usageLine(amount = 1_000L)
                    val usage = PointUsage.use(orderNumber(), listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = 100L)))

                    val snapshot = usage.cancellationLines.toMutableList()
                    snapshot.clear()

                    usage.cancellationLines.size shouldBe 1
                }
            }
        }

        given("PointUsage의 동등성을 비교할 때") {
            `when`("동일한 인스턴스를 비교하면") {
                then("동등하다") {
                    val usage = pointUsage()

                    (usage == usage) shouldBe true
                }
            }

            `when`("서로 다른 id를 가진 두 PointUsage를 비교하면") {
                then("동등하지 않다") {
                    (pointUsage() == pointUsage()) shouldBe false
                }
            }

            `when`("PointUsage가 아닌 타입과 비교하면") {
                then("동등하지 않다") {
                    (pointUsage().equals("not a PointUsage")) shouldBe false
                }
            }

            `when`("hashCode를 조회하면") {
                then("id의 hashCode와 같다") {
                    val usage = pointUsage()

                    usage.hashCode() shouldBe usage.id.hashCode()
                }
            }

            `when`("toString을 조회하면") {
                then("주요 필드 값을 포함한다") {
                    val usage = pointUsage()

                    usage.toString() shouldBe
                        "PointUsage(id=${usage.id}, orderNumber=${usage.orderNumber}, " +
                        "totalAmount=${usage.totalAmount}, status=${usage.status})"
                }
            }
        }
    })
