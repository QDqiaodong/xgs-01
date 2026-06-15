package com.swapmarket.service;

import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.ShareRecord;
import com.swapmarket.mapper.ItemMapper;
import com.swapmarket.mapper.ShareRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordMapper shareRecordMapper;
    private final ItemMapper itemMapper;
    private final RedisCacheService redisCacheService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SHARE_USER_KEY_PREFIX = "swap:share:user:";
    private static final String SHARE_PENDING_KEY = "swap:share:pending";
    private static final long SHARE_DEDUP_EXPIRE_HOURS = 1;

    @Transactional
    public boolean recordShare(Long itemId, Long sharerUserId, String shareChannel, String shareType) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        if (!"published".equals(item.getStatus())) {
            throw new RuntimeException("物品已下架，无法分享");
        }

        String userKey = SHARE_USER_KEY_PREFIX + itemId + ":" + sharerUserId;
        Long added = stringRedisTemplate.opsForSet().add(userKey, sharerUserId.toString());
        if (added != null && added > 0) {
            stringRedisTemplate.expire(userKey, SHARE_DEDUP_EXPIRE_HOURS, TimeUnit.HOURS);
            incrementPendingShareCount(itemId);
        }

        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setItemId(itemId);
        shareRecord.setSharerUserId(sharerUserId);
        shareRecord.setShareChannel(shareChannel);
        shareRecord.setShareType(shareType != null ? shareType : "link");
        shareRecordMapper.insert(shareRecord);

        clearItemRelatedCaches(itemId);
        return true;
    }

    @Transactional
    public boolean recordShareVisit(Long itemId, Long sharerUserId, Long visitorUserId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            return false;
        }

        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setItemId(itemId);
        shareRecord.setSharerUserId(sharerUserId);
        shareRecord.setVisitorUserId(visitorUserId);
        shareRecord.setShareType("visit");
        shareRecordMapper.insert(shareRecord);

        return true;
    }

    private void incrementPendingShareCount(Long itemId) {
        stringRedisTemplate.opsForHash().increment(SHARE_PENDING_KEY, itemId.toString(), 1);
    }

    private void clearItemRelatedCaches(Long itemId) {
        if (itemId != null) {
            redisCacheService.delete(CacheKeyConstants.ITEM_DETAIL_KEY + itemId);
        }
        redisCacheService.delete(CacheKeyConstants.TOP_ITEMS_KEY);
        redisCacheService.deleteByPattern(CacheKeyConstants.LIKE_RANKING_KEY + "*");
        redisCacheService.deleteByPattern(CacheKeyConstants.STATISTICS_DASHBOARD_KEY + "*");
    }
}
