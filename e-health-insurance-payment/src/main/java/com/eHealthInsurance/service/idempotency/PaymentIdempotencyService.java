package com.eHealthInsurance.service.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentIdempotencyService {

    private static final String PAYMENT_PREFIX = "payment:idempotency:";
    private static final String LOCK_PREFIX = "payment:idempotency-lock:";

    private final StringRedisTemplate redisTemplate;

    @Value("${ehi.payment.idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    @Value("${ehi.payment.idempotency.lock-ttl-seconds:300}")
    private long lockTtlSeconds;

    public Optional<UUID> findPaymentId(UUID memberId, String idempotencyKey) {
        try {
            String value = redisTemplate.opsForValue().get(paymentKey(memberId, idempotencyKey));
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (RuntimeException ex) {
            log.warn("Redis idempotency lookup failed, falling back to database uniqueness: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean acquireLock(UUID memberId, String idempotencyKey) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey(memberId, idempotencyKey), "LOCKED", Duration.ofSeconds(lockTtlSeconds));
            return acquired == null || acquired;
        } catch (RuntimeException ex) {
            log.warn("Redis idempotency lock failed, falling back to database uniqueness: {}", ex.getMessage());
            return true;
        }
    }

    public void rememberPayment(UUID memberId, String idempotencyKey, UUID paymentId) {
        try {
            redisTemplate.opsForValue().set(
                    paymentKey(memberId, idempotencyKey),
                    paymentId.toString(),
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (RuntimeException ex) {
            log.warn("Redis idempotency write failed: {}", ex.getMessage());
        }
    }

    public void releaseLock(UUID memberId, String idempotencyKey) {
        try {
            redisTemplate.delete(lockKey(memberId, idempotencyKey));
        } catch (DataAccessException ex) {
            log.warn("Redis idempotency lock release failed: {}", ex.getMessage());
        }
    }

    private String paymentKey(UUID memberId, String idempotencyKey) {
        return PAYMENT_PREFIX + memberId + ":" + idempotencyKey;
    }

    private String lockKey(UUID memberId, String idempotencyKey) {
        return LOCK_PREFIX + memberId + ":" + idempotencyKey;
    }
}
