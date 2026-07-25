package com.jysohn0825.point.support.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.Ordered
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.time.Duration

/**
 * @Transactional의 기본 어드바이저 순서(LOWEST_PRECEDENCE)보다 낮은 값을 줘서
 * 락이 트랜잭션 바깥을 감싸도록 한다 (락 획득 → 트랜잭션 시작 → 커밋 → 락 해제 순서 보장).
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
class DistributedLockAspect(
    private val lockExecutor: DistributedLockExecutor,
) {
    private val parser: SpelExpressionParser = SpelExpressionParser()
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(distributedLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock,
    ): Any? {
        val key: String = resolveKey(joinPoint = joinPoint, keyExpression = distributedLock.key)

        return lockExecutor.executeWithLock(
            key = key,
            waitTime = Duration.ofSeconds(distributedLock.waitTimeSeconds),
            leaseTime = Duration.ofSeconds(distributedLock.leaseTimeSeconds),
        ) {
            joinPoint.proceed()
        }
    }

    private fun resolveKey(
        joinPoint: ProceedingJoinPoint,
        keyExpression: String,
    ): String {
        val method: Method = (joinPoint.signature as MethodSignature).method
        val parameterNames: Array<String> = parameterNameDiscoverer.getParameterNames(method) ?: emptyArray()

        val context: StandardEvaluationContext = StandardEvaluationContext()
        parameterNames.forEachIndexed { index, name ->
            context.setVariable(name, joinPoint.args[index])
        }

        return requireNotNull(parser.parseExpression(keyExpression).getValue(context, String::class.java)) {
            "락 key SpEL 표현식 평가 결과가 null입니다: $keyExpression"
        }
    }
}
