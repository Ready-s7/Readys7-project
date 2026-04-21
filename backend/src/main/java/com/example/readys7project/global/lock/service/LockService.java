package com.example.readys7project.global.lock.service;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final RedissonClient redissonClient;

    private static final long WAIT_TIME_SECONDS = 3L; // 락 획득 최대 대기 기간
    private static final long LEASE_TIME_SECONDS = 5L; // 락 자동 해제 TTL

    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        String lockKey = "lock:" + key;

        // Redisson이 내부적으로 Spin Lock + TTL + Lua Script 전부 처리
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // waitTime: 최대 3초 대시
            // leaseTime: 5초 후 자동 해제 (서버 장애 대비)
            boolean acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                throw new ProposalException(ErrorCode.PROPOSAL_LOCK_FAILED);
            }

            log.info("[Redisson Lock 획득] key={}, thread={}",
                    lockKey, Thread.currentThread().getName());

            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProposalException(ErrorCode.PROPOSAL_LOCK_FAILED);

        } finally {
            // 본인이 잡은 락인지 확인 후 해제 (Redisson이 내부적으로 처리)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Redisson Lock 해제] key={}", lockKey);
            }
        }

    }
}