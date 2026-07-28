package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.entity.pointUsage
import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.CancellationLine
import com.jysohn0825.point.domain.vo.UsageLine
import com.jysohn0825.point.domain.vo.UsageStatus
import com.jysohn0825.point.domain.vo.cancellationLine
import com.jysohn0825.point.domain.vo.orderNumber
import com.jysohn0825.point.domain.vo.usageLine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PointUsageTest :
    BehaviorSpec({
        given("PointUsage.use()로 사용 건을 생성할 때") {
            `when`("유효한 사용 라인들이 주어지면") {
                then("상태는 USED이고 총액은 라인 합계다") {
                    val lines: List<UsageLine> = listOf(usageLine(amount = BigDecimal(1_000)), usageLine(amount = BigDecimal(2_000)))

                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = lines)

                    usage.lines shouldBe lines
                    usage.totalAmount shouldBe BigDecimal(3_000)
                    usage.remainingAmount shouldBe BigDecimal(3_000)
                    usage.cancelledAmount.signum() shouldBe 0
                    usage.status shouldBe UsageStatus.USED
                    usage.cancellationLines shouldBe emptyList()
                }
            }

            `when`("사용 라인이 비어있으면") {
                then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = emptyList())
                    }
                }
            }

            `when`("동일한 적립건을 참조하는 사용 라인이 여러 개 주어지면") {
                then("모두 허용된다") {
                    val earningId: String = "earning-1"
                    val lines: List<UsageLine> =
                        listOf(
                            usageLine(earningId = earningId, amount = BigDecimal(1_000)),
                            usageLine(earningId = earningId, amount = BigDecimal(2_000)),
                        )

                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = lines)

                    usage.lines shouldBe lines
                    usage.totalAmount shouldBe BigDecimal(3_000)
                }
            }
        }

        given("생성된 PointUsage를 취소(cancel)할 때") {
            `when`("일부 금액만 취소하면") {
                then("상태는 PARTIALLY_CANCELED이고 잔여액이 줄어든다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(400))))

                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                    usage.cancelledAmount shouldBe BigDecimal(400)
                    usage.remainingAmount shouldBe BigDecimal(600)
                    usage.cancellationLines.size shouldBe 1
                }
            }

            `when`("전액을 취소하면") {
                then("상태는 FULLY_CANCELED이고 잔여액은 0이다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(1_000))))

                    usage.status shouldBe UsageStatus.FULLY_CANCELED
                    usage.remainingAmount.signum() shouldBe 0
                }
            }

            `when`("여러 라인으로 구성된 사용 건을 부분 취소한 뒤 나머지를 마저 취소하면") {
                then("전액 취소가 된다") {
                    val lineA: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val lineB: UsageLine = usageLine(amount = BigDecimal(2_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(lineA, lineB))

                    usage.cancel(listOf(cancellationLine(originalLine = lineA, restoredAmount = BigDecimal(1_000))))
                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED

                    usage.cancel(listOf(cancellationLine(originalLine = lineB, restoredAmount = BigDecimal(2_000))))
                    usage.status shouldBe UsageStatus.FULLY_CANCELED
                    usage.cancellationLines.size shouldBe 2
                }
            }

            `when`("같은 라인을 여러 번 나누어 취소하면") {
                then("취소 금액이 누적된다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))

                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(300))))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(300))))

                    usage.cancelledAmount shouldBe BigDecimal(600)
                    usage.remainingAmount shouldBe BigDecimal(400)
                    usage.status shouldBe UsageStatus.PARTIALLY_CANCELED
                }
            }

            `when`("취소 요청 라인이 비어있으면") {
                then("예외가 발생한다") {
                    val usage: PointUsage = pointUsage()

                    shouldThrow<PointDomainException> { usage.cancel(emptyList()) }
                }
            }

            `when`("이미 전액 취소된 사용 건을 다시 취소하려 하면") {
                then("예외가 발생한다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(1_000))))

                    shouldThrow<PointDomainException> {
                        usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal.ONE)))
                    }
                }
            }

            `when`("이 사용 건에 속하지 않은 라인을 취소하려 하면") {
                then("예외가 발생한다") {
                    val usage: PointUsage = pointUsage(lines = listOf(usageLine(amount = BigDecimal(1_000))))
                    val foreignLine: UsageLine = usageLine(amount = BigDecimal(500))

                    shouldThrow<PointDomainException> {
                        usage.cancel(listOf(cancellationLine(originalLine = foreignLine, restoredAmount = BigDecimal(100))))
                    }
                }
            }

            `when`("누적 취소 요청 금액이 해당 라인의 사용 금액을 초과하면") {
                then("예외가 발생하고 이전 취소 내역은 유지된다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(700))))

                    shouldThrow<PointDomainException> {
                        usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(400))))
                    }

                    usage.cancelledAmount shouldBe BigDecimal(700)
                }
            }

            `when`("취소가 반영된 뒤 cancellationLines를 조회하면") {
                then("내부 상태를 보호하는 방어적 복사본을 반환한다") {
                    val line: UsageLine = usageLine(amount = BigDecimal(1_000))
                    val usage: PointUsage = PointUsage.use(id = "usage-1", orderNumber = orderNumber(), lines = listOf(line))
                    usage.cancel(listOf(cancellationLine(originalLine = line, restoredAmount = BigDecimal(100))))

                    val snapshot: MutableList<CancellationLine> = usage.cancellationLines.toMutableList()
                    snapshot.clear()

                    usage.cancellationLines.size shouldBe 1
                }
            }
        }

        given("PointUsage의 동등성을 비교할 때") {
            `when`("동일한 인스턴스를 비교하면") {
                then("동등하다") {
                    val usage: PointUsage = pointUsage()

                    (usage == usage) shouldBe true
                }
            }

            `when`("서로 다른 id를 가진 두 PointUsage를 비교하면") {
                then("동등하지 않다") {
                    (pointUsage(id = "1") == pointUsage(id = "2")) shouldBe false
                }
            }

            `when`("PointUsage가 아닌 타입과 비교하면") {
                then("동등하지 않다") {
                    (pointUsage().equals("not a PointUsage")) shouldBe false
                }
            }

            `when`("hashCode를 조회하면") {
                then("id의 hashCode와 같다") {
                    val usage: PointUsage = pointUsage()

                    usage.hashCode() shouldBe usage.id.hashCode()
                }
            }
        }
    })
