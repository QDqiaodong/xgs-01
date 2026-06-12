package com.swapmarket.task;

import com.swapmarket.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ItemMapper itemMapper;

    private static final String VIEW_PENDING_KEY = "swap:view:pending";

    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDatabase() {
        Map<Object, Object> pendingViews = stringRedisTemplate.opsForHash().entries(VIEW_PENDING_KEY);
        if (pendingViews.isEmpty()) {
            return;
        }

        log.info("开始同步浏览量数据，共 {} 条记录", pendingViews.size());

        Set<Map.Entry<Object, Object>> entries = pendingViews.entrySet();
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Object, Object> entry : entries) {
            try {
                Long itemId = Long.valueOf(entry.getKey().toString());
                Integer increment = Integer.valueOf(entry.getValue().toString());

                itemMapper.updateViewCount(itemId, increment);
                stringRedisTemplate.opsForHash().delete(VIEW_PENDING_KEY, entry.getKey().toString());
                successCount++;
            } catch (Exception e) {
                log.error("同步物品浏览量失败, itemId={}, increment={}", entry.getKey(), entry.getValue(), e);
                failCount++;
            }
        }

        log.info("浏览量同步完成，成功: {}, 失败: {}", successCount, failCount);
    }
}
