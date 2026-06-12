package com.swapmarket.task;

import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.service.CategoryService;
import com.swapmarket.service.ItemService;
import com.swapmarket.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements CommandLineRunner {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final RedisCacheService redisCacheService;

    @Override
    public void run(String... args) {
        log.info("开始缓存预热...");

        try {
            Boolean lockAcquired = redisCacheService.setIfAbsent(
                    CacheKeyConstants.CACHE_WARMUP_LOCK_KEY,
                    "true",
                    CacheKeyConstants.CACHE_WARMUP_LOCK_TTL,
                    CacheKeyConstants.CACHE_WARMUP_LOCK_TTL_UNIT
            );

            if (Boolean.FALSE.equals(lockAcquired)) {
                log.info("其他实例正在进行缓存预热，跳过");
                return;
            }

            warmupCategoryList();
            warmupTopItems();
            warmupLikeRanking();

            log.info("缓存预热完成");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }

    private void warmupCategoryList() {
        log.info("预热分类列表缓存");
        categoryService.list();
        log.info("分类列表缓存预热完成");
    }

    private void warmupTopItems() {
        log.info("预热置顶物品缓存");
        itemService.getTopItems();
        log.info("置顶物品缓存预热完成");
    }

    private void warmupLikeRanking() {
        log.info("预热点赞排行榜缓存");
        itemService.getLikeRanking(10, null);
        itemService.getLikeRanking(20, null);
        itemService.getLikeRanking(50, null);
        log.info("点赞排行榜缓存预热完成");
    }
}
