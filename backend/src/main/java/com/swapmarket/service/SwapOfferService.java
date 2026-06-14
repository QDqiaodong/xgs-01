package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.entity.*;
import com.swapmarket.enums.SwapOfferStatus;
import com.swapmarket.exception.BusinessException;
import com.swapmarket.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwapOfferService {
    private final SwapOfferMapper swapOfferMapper;
    private final SwapOfferStatusLogMapper swapOfferStatusLogMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final RedisCacheService redisCacheService;

    @Transactional
    public SwapOffer createOffer(Long fromUserId, Long fromItemId, Long toItemId, String message) {
        Item fromItem = itemMapper.selectById(fromItemId);
        Item toItem = itemMapper.selectById(toItemId);

        if (fromItem == null || toItem == null) {
            throw new RuntimeException("物品不存在");
        }

        if (!fromItem.getUserId().equals(fromUserId)) {
            throw new RuntimeException("您不是该物品的所有者，无法使用该物品发起邀约");
        }

        if (toItem.getUserId().equals(fromUserId)) {
            throw new RuntimeException("不能对自己的物品发起互换邀约");
        }

        if (!"published".equals(fromItem.getStatus())) {
            throw new RuntimeException("您的物品已下架或已成交，无法发起互换邀约");
        }

        if (!"published".equals(toItem.getStatus())) {
            if ("offline".equals(toItem.getStatus())) {
                throw new RuntimeException("对方物品已下架，无法发起互换邀约");
            } else if ("completed".equals(toItem.getStatus())) {
                throw new RuntimeException("对方物品已成交，无法发起互换邀约");
            } else {
                throw new RuntimeException("对方物品当前不可互换");
            }
        }

        SwapOffer offer = new SwapOffer();
        offer.setFromUserId(fromUserId);
        offer.setToUserId(toItem.getUserId());
        offer.setFromItemId(fromItemId);
        offer.setToItemId(toItemId);
        offer.setMessage(message);
        offer.setStatus(SwapOfferStatus.PENDING.getCode());
        swapOfferMapper.insert(offer);

        User fromUser = userMapper.selectById(fromUserId);
        recordStatusLog(offer.getId(), null, SwapOfferStatus.PENDING.getCode(),
                fromUserId, fromUser != null ? fromUser.getNickname() : null, "发起互换邀约");
        log.info("用户[{}]发起互换邀约，邀约ID: {}", fromUserId, offer.getId());

        notificationService.createNewOfferNotification(
                toItem.getUserId(),
                fromUserId,
                offer.getId(),
                toItemId,
                fromItem.getTitle(),
                toItem.getTitle()
        );

        clearUserOfferCaches(toItem.getUserId());
        return offer;
    }

    public List<SwapOffer> getReceivedOffers(Long userId) {
        String key = CacheKeyConstants.USER_PENDING_OFFERS_KEY + userId;
        List<SwapOffer> cached = redisCacheService.getList(key, SwapOffer.class);
        if (cached != null) {
            return cached;
        }

        List<SwapOffer> offers = swapOfferMapper.selectList(new LambdaQueryWrapper<SwapOffer>()
                .eq(SwapOffer::getToUserId, userId)
                .orderByDesc(SwapOffer::getCreateTime));

        enrichOffers(offers);

        redisCacheService.set(key, offers,
                CacheKeyConstants.USER_PENDING_OFFERS_TTL, CacheKeyConstants.USER_PENDING_OFFERS_TTL_UNIT);
        return offers;
    }

    public List<SwapOffer> getSentOffers(Long userId) {
        List<SwapOffer> offers = swapOfferMapper.selectList(new LambdaQueryWrapper<SwapOffer>()
                .eq(SwapOffer::getFromUserId, userId)
                .orderByDesc(SwapOffer::getCreateTime));
        enrichOffers(offers);
        return offers;
    }

    @Transactional
    public void acceptOffer(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null || !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("邀约不存在或无权操作");
        }

        validateStatusTransition(offer.getStatus(), SwapOfferStatus.ACCEPTED.getCode());

        String oldStatus = offer.getStatus();
        offer.setStatus(SwapOfferStatus.ACCEPTED.getCode());
        swapOfferMapper.updateById(offer);

        User operator = userMapper.selectById(userId);
        recordStatusLog(offer.getId(), oldStatus, SwapOfferStatus.ACCEPTED.getCode(),
                userId, operator != null ? operator.getNickname() : null, "接受互换邀约");
        log.info("用户[{}]接受互换邀约，邀约ID: {}，状态: {} -> {}",
                userId, offerId, oldStatus, SwapOfferStatus.ACCEPTED.getCode());

        Item fromItem = itemMapper.selectById(offer.getFromItemId());
        Item toItem = itemMapper.selectById(offer.getToItemId());
        if (fromItem != null) {
            fromItem.setStatus("completed");
            itemMapper.updateById(fromItem);
        }
        if (toItem != null) {
            toItem.setStatus("completed");
            itemMapper.updateById(toItem);
        }

        notificationService.createOfferAcceptedNotification(
                offer.getFromUserId(),
                offer.getId(),
                offer.getToItemId(),
                fromItem != null ? fromItem.getTitle() : "",
                toItem != null ? toItem.getTitle() : ""
        );

        clearUserOfferCaches(userId);
        clearUserOfferCaches(offer.getFromUserId());
    }

    @Transactional
    public void rejectOffer(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null || !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("邀约不存在或无权操作");
        }

        validateStatusTransition(offer.getStatus(), SwapOfferStatus.REJECTED.getCode());

        String oldStatus = offer.getStatus();
        offer.setStatus(SwapOfferStatus.REJECTED.getCode());
        swapOfferMapper.updateById(offer);

        User operator = userMapper.selectById(userId);
        recordStatusLog(offer.getId(), oldStatus, SwapOfferStatus.REJECTED.getCode(),
                userId, operator != null ? operator.getNickname() : null, "驳回互换邀约");
        log.info("用户[{}]驳回互换邀约，邀约ID: {}，状态: {} -> {}",
                userId, offerId, oldStatus, SwapOfferStatus.REJECTED.getCode());

        Item fromItem = itemMapper.selectById(offer.getFromItemId());
        Item toItem = itemMapper.selectById(offer.getToItemId());

        notificationService.createOfferRejectedNotification(
                offer.getFromUserId(),
                offer.getId(),
                offer.getToItemId(),
                fromItem != null ? fromItem.getTitle() : "",
                toItem != null ? toItem.getTitle() : ""
        );
        
        clearUserOfferCaches(userId);
        clearUserOfferCaches(offer.getFromUserId());
    }

    public SwapOffer getOfferDetail(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }
        if (!offer.getFromUserId().equals(userId) && !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("无权查看该邀约");
        }
        enrichOffers(Collections.singletonList(offer));
        return offer;
    }

    private void enrichOffers(List<SwapOffer> offers) {
        if (offers.isEmpty()) return;

        Set<Long> itemIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (SwapOffer offer : offers) {
            if (offer.getFromItemId() != null) itemIds.add(offer.getFromItemId());
            if (offer.getToItemId() != null) itemIds.add(offer.getToItemId());
            if (offer.getFromUserId() != null) userIds.add(offer.getFromUserId());
        }

        Map<Long, Item> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<Item> items = itemMapper.selectBatchIds(itemIds);
            Map<Long, List<String>> imageMap = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                            .in(ItemImage::getItemId, itemIds)
                            .orderByAsc(ItemImage::getSortOrder))
                    .stream()
                    .collect(Collectors.groupingBy(
                            ItemImage::getItemId,
                            Collectors.mapping(ItemImage::getImageUrl, Collectors.toList())
                    ));
            for (Item item : items) {
                item.setImages(imageMap.getOrDefault(item.getId(), new ArrayList<>()));
                itemMap.put(item.getId(), item);
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        for (SwapOffer offer : offers) {
            offer.setFromItem(itemMap.get(offer.getFromItemId()));
            offer.setToItem(itemMap.get(offer.getToItemId()));
            offer.setFromUser(userMap.get(offer.getFromUserId()));
        }
    }

    public int getPendingOfferCount(Long userId) {
        String countKey = CacheKeyConstants.USER_OFFER_COUNT_KEY + userId;
        Object cached = redisCacheService.get(countKey);
        if (cached != null) {
            return ((Number) cached).intValue();
        }

        long count = swapOfferMapper.selectCount(new LambdaQueryWrapper<SwapOffer>()
                .eq(SwapOffer::getToUserId, userId)
                .eq(SwapOffer::getStatus, SwapOfferStatus.PENDING.getCode()));

        int result = (int) count;
        redisCacheService.set(countKey, result,
                CacheKeyConstants.USER_OFFER_COUNT_TTL, CacheKeyConstants.USER_OFFER_COUNT_TTL_UNIT);
        return result;
    }

    private void clearUserOfferCaches(Long userId) {
        if (userId == null) {
            return;
        }
        redisCacheService.delete(CacheKeyConstants.USER_PENDING_OFFERS_KEY + userId);
        redisCacheService.delete(CacheKeyConstants.USER_OFFER_COUNT_KEY + userId);
        redisCacheService.deleteByPattern(CacheKeyConstants.STATISTICS_DASHBOARD_KEY + "*");
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        SwapOfferStatus current = SwapOfferStatus.fromCode(currentStatus);
        SwapOfferStatus target = SwapOfferStatus.fromCode(targetStatus);

        if (current == null) {
            throw BusinessException.invalidStatusTransition(currentStatus, targetStatus);
        }
        if (target == null) {
            throw new BusinessException("无效的目标状态: " + targetStatus);
        }
        if (!current.canTransitionTo(target)) {
            throw BusinessException.invalidStatusTransition(currentStatus, targetStatus);
        }
    }

    private void recordStatusLog(Long offerId, String fromStatus, String toStatus,
                                  Long operatorId, String operatorName, String remark) {
        try {
            SwapOfferStatusLog statusLog = SwapOfferStatusLog.of(
                    offerId, fromStatus, toStatus, operatorId, operatorName, remark);
            swapOfferStatusLogMapper.insert(statusLog);
        } catch (Exception e) {
            log.error("记录邀约状态变更日志失败，offerId: {}, fromStatus: {}, toStatus: {}",
                    offerId, fromStatus, toStatus, e);
        }
    }
}
