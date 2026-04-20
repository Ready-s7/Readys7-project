package com.example.readys7project.global.lock.service;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import com.example.readys7project.global.lock.repository.LockRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private final LockRedisRepository lockRedisRepository;

    private static final long LOCK_TIMEOUT_SECONDS = 5L;
    private static final long RETRY_INTERVAL_MS = 50L;   // 재시도 간격
    private static final long MAX_WAIT_MS = 3000L;        // 최대 대기 시간

    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();

        // 락 획득될 때까지 재시도 (최대 3초 대기)
        while (!lockRedisRepository.lock(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {

            // 최대 대기 시간 초과 시 예외
            if (System.currentTimeMillis() - startTime > MAX_WAIT_MS) {
                throw new ProposalException(ErrorCode.PROPOSAL_LOCK_FAILED);
            }

            try {
                Thread.sleep(RETRY_INTERVAL_MS); // 50ms 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProposalException(ErrorCode.PROPOSAL_LOCK_FAILED);
            }
        }

        try {
            return supplier.get();
        } finally {
            lockRedisRepository.unlock(lockKey, lockValue);
        }
    }
}