package com.swapmarket.common;

public class CacheKeyConstants {

    public static final String ITEM_DETAIL_KEY = "swap:item:detail:";
    public static final long ITEM_DETAIL_TTL = 30;
    public static final java.util.concurrent.TimeUnit ITEM_DETAIL_TTL_UNIT = java.util.concurrent.TimeUnit.MINUTES;

    public static final String CATEGORY_LIST_KEY = "swap:category:list";
    public static final long CATEGORY_LIST_TTL = 2;
    public static final java.util.concurrent.TimeUnit CATEGORY_LIST_TTL_UNIT = java.util.concurrent.TimeUnit.HOURS;

    public static final String TOP_ITEMS_KEY = "swap:items:top";
    public static final long TOP_ITEMS_TTL = 1;
    public static final java.util.concurrent.TimeUnit TOP_ITEMS_TTL_UNIT = java.util.concurrent.TimeUnit.HOURS;

    public static final String USER_PENDING_OFFERS_KEY = "swap:offers:pending:";
    public static final long USER_PENDING_OFFERS_TTL = 30;
    public static final java.util.concurrent.TimeUnit USER_PENDING_OFFERS_TTL_UNIT = java.util.concurrent.TimeUnit.MINUTES;

    public static final String USER_OFFER_COUNT_KEY = "swap:user:offer:count:";
    public static final long USER_OFFER_COUNT_TTL = 10;
    public static final java.util.concurrent.TimeUnit USER_OFFER_COUNT_TTL_UNIT = java.util.concurrent.TimeUnit.MINUTES;

    public static final String LIKE_RANKING_KEY = "swap:items:like:ranking";
    public static final long LIKE_RANKING_TTL = 15;
    public static final java.util.concurrent.TimeUnit LIKE_RANKING_TTL_UNIT = java.util.concurrent.TimeUnit.MINUTES;

    public static final String CACHE_WARMUP_LOCK_KEY = "swap:cache:warmup:lock";
    public static final long CACHE_WARMUP_LOCK_TTL = 60;
    public static final java.util.concurrent.TimeUnit CACHE_WARMUP_LOCK_TTL_UNIT = java.util.concurrent.TimeUnit.SECONDS;
}
