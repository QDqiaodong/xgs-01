package com.swapmarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public void increment(String key, long delta) {
        redisTemplate.opsForValue().increment(key, delta);
    }

    public void decrement(String key, long delta) {
        redisTemplate.opsForValue().decrement(key, delta);
    }

    public Long incrementAndGet(String key, long delta, long timeout, TimeUnit unit) {
        Long count = redisTemplate.opsForValue().increment(key, delta);
        if (count != null && count == delta) {
            redisTemplate.expire(key, timeout, unit);
        }
        return count;
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> clazz) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        if (clazz.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, Class<T> clazz) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return (Map<K, V>) obj;
        }
        return null;
    }
}
