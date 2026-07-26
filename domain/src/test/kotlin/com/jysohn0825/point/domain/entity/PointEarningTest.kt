package com.jysohn0825.point.domain.entity

import com.jysohn0825.point.domain.exception.PointDomainException
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.EarningStatus
import com.jysohn0825.point.domain.vo.ExpirationPeriod
import com.jysohn0825.point.domain.vo.grantedBy
import com.jysohn0825.point.domain.vo.pointAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class PointEarningTest :
    BehaviorSpec({
        Given("필수 값만 전달하고 나머지는 기본값에 맡기는 경우") {
            When("PointEarning.earn을 호출하면") {
                val earning: PointEarning =
                    PointEarning.earn(
                        amount = pointAmount(BigDecimal(500)),
                        earnType = EarnType.SYSTEM,
                        sourceReferenceId = "ORDER-0001",
                    )

                Then("지급 관리자 없이 기본 만료 기간(365일)이 적용된다") {
                    earning.grantedBy shouldBe null
                    earning.expirationDate.value shouldBe earning.earnedAt.plusDays(ExpirationPeriod.DEFAULT_DAYS)
                }
            }
        }

        Given("SYSTEM 적립을 생성할 때") {
            When("지급 관리자 없이 생성하면") {
                val id: String = UUID.randomUUID().toString()
                val earning: PointEarning = pointEarning(id = id)

                Then("ACTIVE 상태로 생성되고 잔여액이 적립액과 같다") {
                    earning.id shouldBe id
                    earning.earnType shouldBe EarnType.SYSTEM
                    earning.grantedBy shouldBe null
                    earning.status shouldBe EarningStatus.ACTIVE
                    earning.remainingAmount.value shouldBe earning.amount.value
                }
            }

            When("지급 관리자와 함께 생성을 시도하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        pointEarning(earnType = EarnType.SYSTEM, grantedBy = grantedBy())
                    }
                }
            }
        }

        Given("MANUAL 적립을 생성할 때") {
            When("지급 관리자와 함께 생성하면") {
                val earning: PointEarning = pointEarning(earnType = EarnType.MANUAL, grantedBy = grantedBy("admin-9999"))

                Then("지급 관리자 정보가 함께 저장된다") {
                    earning.earnType shouldBe EarnType.MANUAL
                    earning.grantedBy shouldBe grantedBy("admin-9999")
                }
            }

            When("지급 관리자 없이 생성을 시도하면") {
                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        pointEarning(earnType = EarnType.MANUAL, grantedBy = null)
                    }
                }
            }
        }

        Given("만료 여부를 판단할 때") {
            When("만료일 이전 시점으로 확인하면") {
                val earning: PointEarning = pointEarning()

                Then("만료되지 않은 것으로 판단한다") {
                    earning.isExpiredAt(POINT_EARNING_DEFAULT_EARNED_AT) shouldBe false
                }
            }

            When("만료일 이후 시점으로 확인하면") {
                val earning: PointEarning = pointEarning()

                Then("만료된 것으로 판단한다") {
                    earning.isExpiredAt(POINT_EARNING_DEFAULT_EARNED_AT.plusDays(400)) shouldBe true
                }
            }
        }

        Given("전혀 사용되지 않은 ACTIVE 적립건이 있을 때") {
            When("적립취소를 하면") {
                val earning: PointEarning = pointEarning()

                Then("취소 가능하며 CANCELED 상태로 전환된다") {
                    earning.canCancelEarning() shouldBe true

                    earning.cancelEarning()

                    earning.status shouldBe EarningStatus.CANCELED
                    earning.remainingAmount.value.signum() shouldBe 0
                }
            }
        }

        Given("일부가 사용된 적립건이 있을 때") {
            When("적립취소를 시도하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                earning.use(BigDecimal.ONE)

                Then("취소할 수 없다") {
                    earning.canCancelEarning() shouldBe false
                    shouldThrow<PointDomainException> {
                        earning.cancelEarning()
                    }
                }
            }
        }

        Given("이미 취소된 적립건이 있을 때") {
            When("다시 적립취소를 시도하면") {
                val earning: PointEarning = pointEarning()
                earning.cancelEarning()

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earning.cancelEarning()
                    }
                }
            }

            When("사용취소를 시도하면") {
                val earning: PointEarning = pointEarning()
                earning.cancelEarning()

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earning.restoreUsage(BigDecimal.ONE)
                    }
                }
            }

            When("만료 처리를 시도하면") {
                val earning: PointEarning = pointEarning()
                earning.cancelEarning()

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earning.expire()
                    }
                }
            }
        }

        Given("잔여액이 1,000원인 ACTIVE 적립건이 있을 때") {
            When("잔여액 이내로 사용하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))

                earning.use(BigDecimal(300))

                Then("잔여액이 차감되고 ACTIVE 상태를 유지한다") {
                    earning.remainingAmount.value shouldBe BigDecimal(700)
                    earning.status shouldBe EarningStatus.ACTIVE
                }
            }

            When("잔여액을 모두 사용하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))

                earning.use(BigDecimal(1_000))

                Then("EXHAUSTED 상태가 된다") {
                    earning.remainingAmount.value.signum() shouldBe 0
                    earning.status shouldBe EarningStatus.EXHAUSTED
                }
            }
        }

        Given("ACTIVE 상태가 아닌 적립건이 있을 때") {
            When("사용을 시도하면") {
                val earning: PointEarning = pointEarning()
                earning.cancelEarning()

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earning.use(BigDecimal.ONE)
                    }
                }
            }
        }

        Given("잔여액을 모두 사용한 EXHAUSTED 적립건이 있을 때") {
            When("사용취소로 일부를 복원하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                earning.use(BigDecimal(1_000))

                earning.restoreUsage(BigDecimal(400))

                Then("ACTIVE 상태로 돌아오고 잔여액이 복원된다") {
                    earning.status shouldBe EarningStatus.ACTIVE
                    earning.remainingAmount.value shouldBe BigDecimal(400)
                }
            }
        }

        Given("일부만 사용된 ACTIVE 적립건이 있을 때") {
            When("사용취소로 일부를 복원하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                earning.use(BigDecimal(300))

                earning.restoreUsage(BigDecimal(100))

                Then("잔여액이 복원된다") {
                    earning.status shouldBe EarningStatus.ACTIVE
                    earning.remainingAmount.value shouldBe BigDecimal(800)
                }
            }

            When("최초 적립액을 초과하여 복원을 시도하면") {
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                earning.use(BigDecimal(300))

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        earning.restoreUsage(BigDecimal(301))
                    }
                }
            }
        }

        Given("ACTIVE 상태의 적립건이 있을 때") {
            When("만료 처리를 하면") {
                val earning: PointEarning = pointEarning()

                earning.expire()

                Then("EXPIRED 상태가 된다") {
                    earning.status shouldBe EarningStatus.EXPIRED
                }
            }
        }

        Given("잔여액이 남은 ACTIVE 적립건이 있을 때") {
            When("강제 즉시 만료를 처리하면") {
                val now: LocalDateTime = LocalDateTime.now()
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))

                val voidedAmount: BigDecimal = earning.expireImmediately(now = now)

                Then("잔여액이 소멸되고 EXPIRED 상태가 되며, 소멸액이 반환된다") {
                    voidedAmount shouldBe BigDecimal(1_000)
                    earning.remainingAmount.value shouldBe BigDecimal.ZERO
                    earning.status shouldBe EarningStatus.EXPIRED
                    earning.expirationDate.value shouldBe now
                }
            }
        }

        Given("전액 사용되어 EXHAUSTED 상태인 적립건이 있을 때") {
            When("강제 즉시 만료를 처리하면") {
                val now: LocalDateTime = LocalDateTime.now()
                val earning: PointEarning = pointEarning(amount = pointAmount(BigDecimal(1_000)))
                earning.use(BigDecimal(1_000))

                val voidedAmount: BigDecimal = earning.expireImmediately(now = now)

                Then("소멸할 잔여액이 없어 상태는 EXHAUSTED로 유지되지만 만료일은 앞당겨진다") {
                    voidedAmount shouldBe BigDecimal.ZERO
                    earning.status shouldBe EarningStatus.EXHAUSTED
                    earning.expirationDate.value shouldBe now
                }
            }
        }

        Given("이미 취소되었거나 만료된 적립건이 있을 때") {
            When("강제 즉시 만료를 시도하면") {
                val canceled: PointEarning = pointEarning()
                canceled.cancelEarning()

                Then("예외가 발생한다") {
                    shouldThrow<PointDomainException> {
                        canceled.expireImmediately()
                    }
                }
            }
        }

        Given("동일한 식별자를 가진 두 적립건이 있을 때") {
            val id: String = UUID.randomUUID().toString()

            When("서로 다른 상태를 갖더라도") {
                val earning1: PointEarning = pointEarning(id = id)
                val earning2: PointEarning = pointEarning(id = id, amount = pointAmount(BigDecimal(2_000)))
                earning2.use(BigDecimal(500))

                Then("동일한 적립건으로 취급한다") {
                    earning1 shouldBe earning2
                    earning1.hashCode() shouldBe earning2.hashCode()
                }
            }
        }

        Given("식별자가 다른 두 적립건이 있을 때") {
            When("나머지 값이 모두 같더라도") {
                val earning1: PointEarning = pointEarning(id = UUID.randomUUID().toString())
                val earning2: PointEarning = pointEarning(id = UUID.randomUUID().toString())

                Then("서로 다른 적립건으로 취급한다") {
                    earning1 shouldNotBe earning2
                }
            }
        }

        Given("적립건이 있을 때") {
            When("PointEarning이 아닌 다른 타입과 비교하면") {
                val earning: PointEarning = pointEarning()

                Then("동일하지 않은 것으로 판단한다") {
                    (earning.equals("not-a-point-earning")) shouldBe false
                }
            }

            When("자기 자신과 비교하면") {
                val earning: PointEarning = pointEarning()

                Then("동일한 것으로 판단한다") {
                    (earning == earning) shouldBe true
                }
            }
        }
    })
