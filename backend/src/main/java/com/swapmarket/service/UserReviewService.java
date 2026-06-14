package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.entity.*;
import com.swapmarket.enums.SwapOfferStatus;
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
public class UserReviewService {
    private final UserReviewMapper userReviewMapper;
    private final SwapOfferMapper swapOfferMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final NotificationService notificationService;
    private final RedisCacheService redisCacheService;

    @Transactional
    public UserReview createReview(Long reviewerId, Long offerId, Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1-5星之间");
        }

        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }

        if (!SwapOfferStatus.ACCEPTED.getCode().equals(offer.getStatus())) {
            throw new RuntimeException("只有已接受的邀约才能评价");
        }

        if (!offer.getFromUserId().equals(reviewerId) && !offer.getToUserId().equals(reviewerId)) {
            throw new RuntimeException("您不是该邀约的参与者，无法评价");
        }

        Long targetUserId = offer.getFromUserId().equals(reviewerId)
                ? offer.getToUserId()
                : offer.getFromUserId();

        UserReview existingReview = userReviewMapper.selectOne(new LambdaQueryWrapper<UserReview>()
                .eq(UserReview::getOfferId, offerId)
                .eq(UserReview::getReviewerId, reviewerId));
        if (existingReview != null) {
            throw new RuntimeException("您已经评价过该邀约了");
        }

        UserReview review = new UserReview();
        review.setOfferId(offerId);
        review.setReviewerId(reviewerId);
        review.setTargetUserId(targetUserId);
        review.setRating(rating);
        review.setContent(content);
        userReviewMapper.insert(review);

        updateUserCreditScore(targetUserId);

        User reviewer = userMapper.selectById(reviewerId);
        notificationService.createReviewNotification(
                targetUserId,
                reviewerId,
                offerId,
                reviewer != null ? reviewer.getNickname() : "",
                rating
        );

        clearUserReviewCaches(targetUserId);
        clearUserReviewCaches(reviewerId);

        log.info("用户[{}]对邀约[{}]的用户[{}]进行了评价，评分：{}", reviewerId, offerId, targetUserId, rating);
        return review;
    }

    public List<UserReview> getUserReviews(Long userId) {
        String cacheKey = CacheKeyConstants.USER_REVIEWS_KEY + userId;
        List<UserReview> cached = redisCacheService.getList(cacheKey, UserReview.class);
        if (cached != null) {
            return cached;
        }

        List<UserReview> reviews = userReviewMapper.selectList(new LambdaQueryWrapper<UserReview>()
                .eq(UserReview::getTargetUserId, userId)
                .orderByDesc(UserReview::getCreateTime));

        enrichReviews(reviews);

        redisCacheService.set(cacheKey, reviews,
                CacheKeyConstants.USER_REVIEWS_TTL, CacheKeyConstants.USER_REVIEWS_TTL_UNIT);

        return reviews;
    }

    public List<UserReview> getRecentReviews(Long userId, int limit) {
        List<UserReview> allReviews = getUserReviews(userId);
        return allReviews.stream().limit(limit).collect(Collectors.toList());
    }

    public Map<String, Object> getUserCreditInfo(Long userId) {
        String cacheKey = CacheKeyConstants.USER_CREDIT_KEY + userId;
        Map<String, Object> cached = redisCacheService.getMap(cacheKey, String.class, Object.class);
        if (cached != null) {
            return cached;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("creditScore", user.getCreditScore() != null ? user.getCreditScore() : 5.0);
        result.put("reviewCount", user.getReviewCount() != null ? user.getReviewCount() : 0);

        redisCacheService.set(cacheKey, result,
                CacheKeyConstants.USER_CREDIT_TTL, CacheKeyConstants.USER_CREDIT_TTL_UNIT);

        return result;
    }

    public boolean hasReviewed(Long offerId, Long userId) {
        Long count = userReviewMapper.selectCount(new LambdaQueryWrapper<UserReview>()
                .eq(UserReview::getOfferId, offerId)
                .eq(UserReview::getReviewerId, userId));
        return count != null && count > 0;
    }

    public Map<String, Object> getOfferReviewStatus(Long offerId, Long userId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }

        Map<String, Object> result = new HashMap<>();
        boolean canReview = SwapOfferStatus.ACCEPTED.getCode().equals(offer.getStatus());
        result.put("canReview", canReview);

        boolean fromUserReviewed = hasReviewed(offerId, offer.getFromUserId());
        boolean toUserReviewed = hasReviewed(offerId, offer.getToUserId());
        result.put("fromUserReviewed", fromUserReviewed);
        result.put("toUserReviewed", toUserReviewed);

        if (offer.getFromUserId().equals(userId)) {
            result.put("currentUserReviewed", fromUserReviewed);
            result.put("targetUserReviewed", toUserReviewed);
        } else if (offer.getToUserId().equals(userId)) {
            result.put("currentUserReviewed", toUserReviewed);
            result.put("targetUserReviewed", fromUserReviewed);
        } else {
            result.put("currentUserReviewed", false);
            result.put("targetUserReviewed", false);
        }

        UserReview myReview = userReviewMapper.selectOne(new LambdaQueryWrapper<UserReview>()
                .eq(UserReview::getOfferId, offerId)
                .eq(UserReview::getReviewerId, userId));
        result.put("myReview", myReview);

        UserReview targetReview = null;
        Long targetUserId = offer.getFromUserId().equals(userId) ? offer.getToUserId() : offer.getFromUserId();
        if (!offer.getFromUserId().equals(userId) || !offer.getToUserId().equals(userId)) {
            targetReview = userReviewMapper.selectOne(new LambdaQueryWrapper<UserReview>()
                    .eq(UserReview::getOfferId, offerId)
                    .eq(UserReview::getReviewerId, targetUserId));
        }
        result.put("targetReview", targetReview);

        return result;
    }

    private void updateUserCreditScore(Long userId) {
        List<UserReview> reviews = userReviewMapper.selectList(new LambdaQueryWrapper<UserReview>()
                .eq(UserReview::getTargetUserId, userId));

        int reviewCount = reviews.size();
        double avgScore = 5.0;
        if (reviewCount > 0) {
            double totalScore = reviews.stream()
                    .mapToDouble(r -> r.getRating() != null ? r.getRating() : 0)
                    .sum();
            avgScore = totalScore / reviewCount;
        }

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setCreditScore(Math.round(avgScore * 100.0) / 100.0);
            user.setReviewCount(reviewCount);
            userMapper.updateById(user);
        }
    }

    private void enrichReviews(List<UserReview> reviews) {
        if (reviews.isEmpty()) return;

        Set<Long> reviewerIds = new HashSet<>();
        Set<Long> offerIds = new HashSet<>();
        for (UserReview review : reviews) {
            if (review.getReviewerId() != null) reviewerIds.add(review.getReviewerId());
            if (review.getOfferId() != null) offerIds.add(review.getOfferId());
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!reviewerIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(reviewerIds);
            for (User user : users) {
                user.setPassword(null);
                userMap.put(user.getId(), user);
            }
        }

        Map<Long, SwapOffer> offerMap = new HashMap<>();
        Set<Long> itemIds = new HashSet<>();
        if (!offerIds.isEmpty()) {
            List<SwapOffer> offers = swapOfferMapper.selectBatchIds(offerIds);
            for (SwapOffer offer : offers) {
                offerMap.put(offer.getId(), offer);
                if (offer.getFromItemId() != null) itemIds.add(offer.getFromItemId());
                if (offer.getToItemId() != null) itemIds.add(offer.getToItemId());
            }
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

        for (UserReview review : reviews) {
            review.setReviewer(userMap.get(review.getReviewerId()));
            SwapOffer offer = offerMap.get(review.getOfferId());
            if (offer != null) {
                review.setFromItem(itemMap.get(offer.getFromItemId()));
                review.setToItem(itemMap.get(offer.getToItemId()));
            }
        }
    }

    private void clearUserReviewCaches(Long userId) {
        if (userId == null) {
            return;
        }
        redisCacheService.delete(CacheKeyConstants.USER_REVIEWS_KEY + userId);
        redisCacheService.delete(CacheKeyConstants.USER_CREDIT_KEY + userId);
    }
}
