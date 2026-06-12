package com.swapmarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_USER_KEY_PREFIX = "swap:view:user:";
    private static final String VIEW_PENDING_KEY = "swap:view:pending";
    private static final long VIEW_DEDUP_EXPIRE_HOURS = 1;

    public boolean recordView(Long itemId, Long userId) {
        if (userId == null) {
            recordAnonymousView(itemId);
            return true;
        }
        String userKey = VIEW_USER_KEY_PREFIX + itemId;
        Long added = stringRedisTemplate.opsForSet().add(userKey, userId.toString());
        if (added != null && added > 0) {
            stringRedisTemplate.expire(userKey, VIEW_DEDUP_EXPIRE_HOURS, TimeUnit.HOURS);
            incrementPendingCount(itemId);
            return true;
        }
        return false;
    }

    private void recordAnonymousView(Long itemId) {
        incrementPendingCount(itemId);
    }

    private void incrementPendingCount(Long itemId) {
        stringRedisTemplate.opsForHash().increment(VIEW_PENDING_KEY, itemId.toString(), 1);
    }
}
