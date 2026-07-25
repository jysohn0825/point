package com.jysohn0825.point.domain.vo

import com.jysohn0825.point.domain.exception.PointDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GrantedByTest :
    FunSpec({
        test("비어있지 않은 관리자 식별자면 정상적으로 생성된다") {
            val grantedBy: GrantedBy = GrantedBy("admin-0001")

            grantedBy.adminId shouldBe "admin-0001"
        }

        test("빈 문자열이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                GrantedBy("")
            }
        }

        test("공백만 있는 문자열이면 예외가 발생한다") {
            shouldThrow<PointDomainException> {
                GrantedBy("   ")
            }
        }
    })
