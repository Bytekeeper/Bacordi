package org.bytekeeper.bacordi

import com.google.common.util.concurrent.RateLimiter
import org.apache.logging.log4j.LogManager
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(val key: String = "", val callsPerSecond: Double = 1 / 3.0)

@Aspect
@Component
class RateLimitAspect {
    private val log = LogManager.getLogger()
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()

    @Around("@annotation(limit)")
    fun limitRate(jp: ProceedingJoinPoint, limit: RateLimit): Any? {
        val rateLimiter = rateLimiters.computeIfAbsent(limit.key) { key ->
            RateLimiter.create(limit.callsPerSecond)
        }
        if (rateLimiter.tryAcquire((1500 / limit.callsPerSecond).toLong(), TimeUnit.MILLISECONDS))
            return jp.proceed()
        log.info("Too many requests, ignoring...")
        throw RateLimitExceeded(jp.signature.name, limit.key)
    }
}

class RateLimitExceeded(methodName: String, key: String) : Exception("$methodName : $key")
