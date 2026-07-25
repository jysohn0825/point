package com.jysohn0825.point.application.earning

import com.jysohn0825.point.application.exception.PointBusinessException
import com.jysohn0825.point.domain.entity.PointEarning
import com.jysohn0825.point.domain.entity.PointPolicy
import com.jysohn0825.point.domain.entity.PointWallet
import com.jysohn0825.point.domain.entity.pointPolicy
import com.jysohn0825.point.domain.entity.pointWallet
import com.jysohn0825.point.domain.lock.DistributedLockExecutor
import com.jysohn0825.point.domain.repository.PointEarningRepository
import com.jysohn0825.point.domain.repository.PointPolicyRepository
import com.jysohn0825.point.domain.repository.PointWalletRepository
import com.jysohn0825.point.domain.vo.EarnType
import com.jysohn0825.point.domain.vo.maxEarnPerTransaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.math.BigDecimal
import java.time.Duration

private class NoopTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

    override fun commit(status: TransactionStatus) {}

    override fun rollback(status: TransactionStatus) {}
}

private class FakeDistributedLockExecutor : DistributedLockExecutor {
    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        action: () -> T,
    ): T = action()
}

private class FakePointWalletRepository(
    var wallet: PointWallet,
) : PointWalletRepository {
    override fun findByMemberIdForUpdate(memberId: String): PointWallet = wallet

    override fun save(wallet: PointWallet) {
        this.wallet = wallet
    }
}

private class FakePointEarningRepository : PointEarningRepository {
    val earnings = mutableListOf<PointEarning>()

    override fun save(earning: PointEarning) {
        earnings.add(earning)
    }

    override fun saveAll(earnings: List<PointEarning>) {
        this.earnings.addAll(earnings)
    }

    override fun findById(earningId: String): PointEarning = earnings.first { it.id == earningId }

    override fun findRedeemableByMemberId(memberId: String): List<PointEarning> = earnings

    override fun findAllByIds(earningIds: List<String>): List<PointEarning> = earnings.filter { it.id in earningIds }

    override fun findByMemberIdAndEarnTypeAndSourceReferenceId(
        memberId: String,
        earnType: EarnType,
        sourceReferenceId: String,
    ): PointEarning? = earnings.firstOrNull { it.earnType == earnType && it.sourceReferenceId == sourceReferenceId }
}

private class FakePointPolicyRepository(
    var policy: PointPolicy,
) : PointPolicyRepository {
    override fun getCurrent(): PointPolicy = policy

    override fun save(policy: PointPolicy) {
        this.policy = policy
    }
}

private fun service(
    walletRepository: FakePointWalletRepository,
    earningRepository: FakePointEarningRepository = FakePointEarningRepository(),
    policyRepository: FakePointPolicyRepository = FakePointPolicyRepository(pointPolicy()),
    lockExecutor: DistributedLockExecutor = FakeDistributedLockExecutor(),
): EarnPointService =
    EarnPointService(
        walletRepository = walletRepository,
        earningRepository = earningRepository,
        policyRepository = policyRepository,
        lockExecutor = lockExecutor,
        transactionManager = NoopTransactionManager(),
    )

class EarnPointServiceTest :
    BehaviorSpec({
        Given("처음 보는 sourceReferenceId로 적립을 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)

            When("적립을 실행하면") {
                val earning =
                    earnPointService.earn(
                        EarnPointCommand(
                            memberId = "member-1",
                            amount = BigDecimal(1_000),
                            earnType = EarnType.SYSTEM,
                            sourceReferenceId = "ORDER-1",
                        ),
                    )

                Then("새 적립건이 생성되고 지갑 잔액이 증가한다") {
                    earning.amount.value shouldBe BigDecimal(1_000)
                    earning.sourceReferenceId shouldBe "ORDER-1"
                    earningRepository.earnings.size shouldBe 1
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("이미 처리된 sourceReferenceId로 동일한 적립을 다시 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val earningRepository = FakePointEarningRepository()
            val earnPointService = service(walletRepository, earningRepository)
            val command =
                EarnPointCommand(
                    memberId = "member-1",
                    amount = BigDecimal(1_000),
                    earnType = EarnType.SYSTEM,
                    sourceReferenceId = "ORDER-1",
                )

            val firstEarning = earnPointService.earn(command)

            When("같은 요청을 재시도하면") {
                val secondEarning = earnPointService.earn(command)

                Then("새 적립건을 만들지 않고 기존 적립건을 그대로 반환한다") {
                    secondEarning.id shouldBe firstEarning.id
                    earningRepository.earnings.size shouldBe 1
                    walletRepository.wallet.balance.amount shouldBe BigDecimal(1_000)
                }
            }
        }

        Given("1회 적립 한도를 초과하는 금액으로 요청하면") {
            val walletRepository = FakePointWalletRepository(pointWallet())
            val policyRepository = FakePointPolicyRepository(pointPolicy(maxEarnPerTransaction = maxEarnPerTransaction(BigDecimal(10_000))))
            val earnPointService = service(walletRepository, policyRepository = policyRepository)

            When("적립을 시도하면") {
                Then("예외가 발생하고 잔액은 변하지 않는다") {
                    shouldThrow<PointBusinessException> {
                        earnPointService.earn(
                            EarnPointCommand(
                                memberId = "member-1",
                                amount = BigDecimal(10_001),
                                earnType = EarnType.SYSTEM,
                                sourceReferenceId = "ORDER-2",
                            ),
                        )
                    }
                    walletRepository.wallet.balance.amount shouldBe BigDecimal.ZERO
                }
            }
        }
    })
