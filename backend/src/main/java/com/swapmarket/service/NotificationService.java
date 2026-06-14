package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.Notification;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.ItemMapper;
import com.swapmarket.mapper.NotificationMapper;
import com.swapmarket.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String NOTIFICATION_LIST_KEY = "swap:notifications:";
    private static final String NOTIFICATION_UNREAD_KEY = "swap:notifications:unread:";
    private static final Random RANDOM = new Random();

    private static final long BASE_TTL_MINUTES = 5;
    private static final long JITTER_MAX_SECONDS = 60;
    private static final long DOUBLE_DELETE_DELAY_MS = 500;

    public static final String TYPE_NEW_OFFER = "new_offer";
    public static final String TYPE_OFFER_ACCEPTED = "offer_accepted";
    public static final String TYPE_OFFER_REJECTED = "offer_rejected";
    public static final String TYPE_NEW_REVIEW = "new_review";

    private long getTtlWithJitterMinutes() {
        return BASE_TTL_MINUTES;
    }

    private long getTtlWithJitterSeconds() {
        return BASE_TTL_MINUTES * 60 + RANDOM.nextInt((int) JITTER_MAX_SECONDS);
    }

    public List<Notification> getNotifications(Long userId) {
        String key = NOTIFICATION_LIST_KEY + userId;
        @SuppressWarnings("unchecked")
        List<Notification> cached = (List<Notification>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        List<Notification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime)
        );

        redisTemplate.opsForValue().set(key, notifications, getTtlWithJitterSeconds(), TimeUnit.SECONDS);
        return notifications;
    }

    public long getUnreadCount(Long userId) {
        String key = NOTIFICATION_UNREAD_KEY + userId;
        Long cached = (Long) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getReadFlag, false)
        );

        redisTemplate.opsForValue().set(key, count, getTtlWithJitterSeconds(), TimeUnit.SECONDS);
        return count;
    }

    @Transactional
    public void markAsRead(Long userId, Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new RuntimeException("通知不存在或无权操作");
        }
        if (Boolean.TRUE.equals(notification.getReadFlag())) {
            return;
        }
        evictCache(userId);
        notification.setReadFlag(true);
        notificationMapper.updateById(notification);
        scheduleDoubleDelete(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        evictCache(userId);
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getReadFlag, false)
                        .set(Notification::getReadFlag, true)
        );
        scheduleDoubleDelete(userId);
    }

    @Transactional
    public void createNewOfferNotification(Long toUserId, Long fromUserId, Long offerId, Long itemId,
                                           String fromItemTitle, String toItemTitle) {
        User fromUser = userMapper.selectById(fromUserId);
        String fromNickname = fromUser != null ? fromUser.getNickname() : "用户";

        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setType(TYPE_NEW_OFFER);
        notification.setTitle("您收到一条新的互换邀约");
        notification.setContent(fromNickname + " 想用「" + fromItemTitle + "」换您的「" + toItemTitle + "」");
        notification.setOfferId(offerId);
        notification.setItemId(itemId);
        notification.setReadFlag(false);
        evictCache(toUserId);
        notificationMapper.insert(notification);
        scheduleDoubleDelete(toUserId);
    }

    @Transactional
    public void createOfferAcceptedNotification(Long fromUserId, Long offerId, Long itemId,
                                                String fromItemTitle, String toItemTitle) {
        Notification notification = new Notification();
        notification.setUserId(fromUserId);
        notification.setType(TYPE_OFFER_ACCEPTED);
        notification.setTitle("邀约已被接受");
        notification.setContent("您发出的「用 " + fromItemTitle + " 换 " + toItemTitle + "」邀约已被对方接受");
        notification.setOfferId(offerId);
        notification.setItemId(itemId);
        notification.setReadFlag(false);
        evictCache(fromUserId);
        notificationMapper.insert(notification);
        scheduleDoubleDelete(fromUserId);
    }

    @Transactional
    public void createOfferRejectedNotification(Long fromUserId, Long offerId, Long itemId,
                                                String fromItemTitle, String toItemTitle) {
        Notification notification = new Notification();
        notification.setUserId(fromUserId);
        notification.setType(TYPE_OFFER_REJECTED);
        notification.setTitle("邀约被驳回");
        notification.setContent("您发出的「用 " + fromItemTitle + " 换 " + toItemTitle + "」邀约已被对方驳回");
        notification.setOfferId(offerId);
        notification.setItemId(itemId);
        notification.setReadFlag(false);
        evictCache(fromUserId);
        notificationMapper.insert(notification);
        scheduleDoubleDelete(fromUserId);
    }

    @Transactional
    public void createReviewNotification(Long targetUserId, Long reviewerId, Long offerId,
                                         String reviewerName, Integer rating) {
        String starStr = "⭐".repeat(rating != null ? rating : 0);
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setType(TYPE_NEW_REVIEW);
        notification.setTitle("您收到一条新的评价");
        notification.setContent(reviewerName + " 对您的交易给出了评价 " + starStr);
        notification.setOfferId(offerId);
        notification.setReadFlag(false);
        evictCache(targetUserId);
        notificationMapper.insert(notification);
        scheduleDoubleDelete(targetUserId);
    }

    private void evictCache(Long userId) {
        redisTemplate.delete(NOTIFICATION_LIST_KEY + userId);
        redisTemplate.delete(NOTIFICATION_UNREAD_KEY + userId);
    }

    private void scheduleDoubleDelete(Long userId) {
        new Thread(() -> {
            try {
                Thread.sleep(DOUBLE_DELETE_DELAY_MS);
                evictCache(userId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Double delete interrupted for user: {}", userId);
            } catch (Exception e) {
                log.error("Double delete failed for user: {}", userId, e);
            }
        }, "notification-cache-cleaner-" + userId).start();
    }
}
