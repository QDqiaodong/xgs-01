package com.swapmarket.controller;

import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.common.Result;
import com.swapmarket.service.CategoryService;
import com.swapmarket.service.ItemService;
import com.swapmarket.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final RedisCacheService redisCacheService;

    @PostMapping("/warmup")
    public Result<Map<String, Object>> warmup(@RequestParam(defaultValue = "all") String type) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        switch (type) {
            case "all":
                warmupCategoryList(result);
                warmupTopItems(result);
                warmupLikeRanking(result);
                break;
            case "category":
                warmupCategoryList(result);
                break;
            case "topItems":
                warmupTopItems(result);
                break;
            case "likeRanking":
                warmupLikeRanking(result);
                break;
            default:
                return Result.error("不支持的缓存类型: " + type);
        }

        result.put("costMs", System.currentTimeMillis() - startTime);
        return Result.success(result);
    }

    @DeleteMapping("/clear")
    public Result<Map<String, Object>> clear(@RequestParam(defaultValue = "all") String type) {
        Map<String, Object> result = new HashMap<>();

        switch (type) {
            case "all":
                clearAllCaches(result);
                break;
            case "item":
                clearItemCaches(result);
                break;
            case "category":
                clearCategoryCaches(result);
                break;
            case "offer":
                clearOfferCaches(result);
                break;
            default:
                return Result.error("不支持的缓存类型: " + type);
        }

        return Result.success(result);
    }

    @DeleteMapping("/clear/item/{itemId}")
    public Result<Void> clearItemCache(@PathVariable Long itemId) {
        redisCacheService.delete(CacheKeyConstants.ITEM_DETAIL_KEY + itemId);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("topItemsCached", redisCacheService.hasKey(CacheKeyConstants.TOP_ITEMS_KEY));
        stats.put("categoryListCached", redisCacheService.hasKey(CacheKeyConstants.CATEGORY_LIST_KEY));

        if (redisCacheService.hasKey(CacheKeyConstants.TOP_ITEMS_KEY)) {
            stats.put("topItemsTtlSeconds", redisCacheService.getExpire(CacheKeyConstants.TOP_ITEMS_KEY));
        }
        if (redisCacheService.hasKey(CacheKeyConstants.CATEGORY_LIST_KEY)) {
            stats.put("categoryListTtlSeconds", redisCacheService.getExpire(CacheKeyConstants.CATEGORY_LIST_KEY));
        }

        return Result.success(stats);
    }

    private void warmupCategoryList(Map<String, Object> result) {
        categoryService.clearCategoryCache();
        categoryService.list();
        result.put("categoryList", "warmed");
    }

    private void warmupTopItems(Map<String, Object> result) {
        redisCacheService.delete(CacheKeyConstants.TOP_ITEMS_KEY);
        itemService.getTopItems();
        result.put("topItems", "warmed");
    }

    private void warmupLikeRanking(Map<String, Object> result) {
        redisCacheService.deleteByPattern(CacheKeyConstants.LIKE_RANKING_KEY + "*");
        itemService.getLikeRanking(10, null);
        itemService.getLikeRanking(20, null);
        itemService.getLikeRanking(50, null);
        result.put("likeRanking", "warmed");
    }

    private void clearAllCaches(Map<String, Object> result) {
        clearItemCaches(result);
        clearCategoryCaches(result);
        clearOfferCaches(result);
    }

    private void clearItemCaches(Map<String, Object> result) {
        redisCacheService.deleteByPattern(CacheKeyConstants.ITEM_DETAIL_KEY + "*");
        redisCacheService.delete(CacheKeyConstants.TOP_ITEMS_KEY);
        redisCacheService.deleteByPattern(CacheKeyConstants.LIKE_RANKING_KEY + "*");
        result.put("itemCaches", "cleared");
    }

    private void clearCategoryCaches(Map<String, Object> result) {
        categoryService.clearCategoryCache();
        result.put("categoryCaches", "cleared");
    }

    private void clearOfferCaches(Map<String, Object> result) {
        redisCacheService.deleteByPattern(CacheKeyConstants.USER_PENDING_OFFERS_KEY + "*");
        redisCacheService.deleteByPattern(CacheKeyConstants.USER_OFFER_COUNT_KEY + "*");
        result.put("offerCaches", "cleared");
    }
}
