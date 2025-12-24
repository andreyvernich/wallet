package org.example.walletapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {
    private final RedissonClient redissonClient;

    @Value("${wallet.lock.timeout:5000}")
    private long lockTimeout;

    @Value("${wallet.lock.waitTime:3000}")
    private long waitTime;

    public boolean tryLock(UUID walletId) {
        RLock lock = redissonClient.getLock("wallet:" + walletId.toString());
        try {
            boolean acquired = lock.tryLock(waitTime, lockTimeout, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("Lock acquired for wallet: {}", walletId);
            } else {
                log.warn("Failed to acquire lock for wallet: {}", walletId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while trying to acquire lock for wallet: {}", walletId, e);
            return false;
        }
    }

    public void unlock(UUID walletId) {
        RLock lock = redissonClient.getLock("wallet:" + walletId.toString());
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released for wallet: {}", walletId);
        }
    }
}
