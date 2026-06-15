package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.entity.*;
import com.swapmarket.enums.SwapOfferStatus;
import com.swapmarket.exception.BusinessException;
import com.swapmarket.mapper.*;
import com.swapmarket.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final CategoryMapper categoryMapper;
    private final NotificationService notificationService;
    private final RedisCacheService redisCacheService;

    private static final long DEFAULT_OFFER_EXPIRE_HOURS = 72;
    public static final String EXPIRE_REASON_TIMEOUT = "邀约超时未处理，自动失效";
    public static final Long SYSTEM_OPERATOR_ID = 0L;
    public static final String SYSTEM_OPERATOR_NAME = "系统";

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
        offer.setExpireTime(LocalDateTime.now().plusHours(DEFAULT_OFFER_EXPIRE_HOURS));
        swapOfferMapper.insert(offer);

        User fromUser = userMapper.selectById(fromUserId);
        recordStatusLog(offer.getId(), null, SwapOfferStatus.PENDING.getCode(),
                fromUserId, fromUser != null ? fromUser.getNickname() : null, "发起互换邀约，有效期" + DEFAULT_OFFER_EXPIRE_HOURS + "小时");
        log.info("用户[{}]发起互换邀约，邀约ID: {}，有效期至: {}", fromUserId, offer.getId(), offer.getExpireTime());

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

        checkOfferExpired(offer);

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

        checkOfferExpired(offer);

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

    public SwapComparisonVO getComparisonData(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }
        if (!offer.getFromUserId().equals(userId) && !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("无权查看该邀约");
        }

        Item fromItem = itemMapper.selectById(offer.getFromItemId());
        Item toItem = itemMapper.selectById(offer.getToItemId());
        User fromUser = userMapper.selectById(offer.getFromUserId());
        User toUser = userMapper.selectById(offer.getToUserId());

        List<String> fromImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, offer.getFromItemId())
                        .orderByAsc(ItemImage::getSortOrder))
                .stream()
                .map(ItemImage::getImageUrl)
                .collect(Collectors.toList());

        List<String> toImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, offer.getToItemId())
                        .orderByAsc(ItemImage::getSortOrder))
                .stream()
                .map(ItemImage::getImageUrl)
                .collect(Collectors.toList());

        Category fromCategory = fromItem != null ? categoryMapper.selectById(fromItem.getCategoryId()) : null;
        Category toCategory = toItem != null ? categoryMapper.selectById(toItem.getCategoryId()) : null;

        SwapComparisonItemVO fromItemVO = new SwapComparisonItemVO();
        if (fromItem != null) {
            fromItemVO.setItemId(fromItem.getId());
            fromItemVO.setTitle(fromItem.getTitle());
            fromItemVO.setImages(fromImages);
            fromItemVO.setDescription(fromItem.getDescription());
            fromItemVO.setCondition(fromItem.getCondition());
            fromItemVO.setExpectedSwap(fromItem.getExpectedSwap());
            fromItemVO.setCategoryName(fromCategory != null ? fromCategory.getName() : "");
            fromItemVO.setPublisherName(fromUser != null ? fromUser.getNickname() : "");
        }

        SwapComparisonItemVO toItemVO = new SwapComparisonItemVO();
        if (toItem != null) {
            toItemVO.setItemId(toItem.getId());
            toItemVO.setTitle(toItem.getTitle());
            toItemVO.setImages(toImages);
            toItemVO.setDescription(toItem.getDescription());
            toItemVO.setCondition(toItem.getCondition());
            toItemVO.setExpectedSwap(toItem.getExpectedSwap());
            toItemVO.setCategoryName(toCategory != null ? toCategory.getName() : "");
            toItemVO.setPublisherName(toUser != null ? toUser.getNickname() : "");
        }

        List<SwapComparisonChecklistItemVO> checklist = buildChecklist(fromItem, toItem, fromImages, toImages);

        long diffCount = checklist.stream().filter(item -> Boolean.TRUE.equals(item.getIsDifferent())).count();

        SwapComparisonVO vo = new SwapComparisonVO();
        vo.setOfferId(offer.getId());
        vo.setOfferStatus(offer.getStatus());
        vo.setOfferMessage(offer.getMessage());
        vo.setCreateTime(offer.getCreateTime() != null ? offer.getCreateTime().toString().replace("T", " ") : "");
        vo.setFromItem(fromItemVO);
        vo.setToItem(toItemVO);
        vo.setChecklist(checklist);
        vo.setDifferentCount((int) diffCount);
        vo.setTotalCount(checklist.size());
        vo.setFromUserNickname(fromUser != null ? fromUser.getNickname() : "");
        vo.setToUserNickname(toUser != null ? toUser.getNickname() : "");

        return vo;
    }

    private List<SwapComparisonChecklistItemVO> buildChecklist(Item fromItem, Item toItem,
                                                               List<String> fromImages, List<String> toImages) {
        List<SwapComparisonChecklistItemVO> checklist = new ArrayList<>();

        SwapComparisonChecklistItemVO imagesItem = new SwapComparisonChecklistItemVO();
        imagesItem.setFieldName("images");
        imagesItem.setFieldLabel("图片");
        imagesItem.setFromValue(String.join("|", fromImages));
        imagesItem.setToValue(String.join("|", toImages));
        imagesItem.setIsDifferent(fromImages.size() != toImages.size());
        imagesItem.setIsImageField(true);
        checklist.add(imagesItem);

        SwapComparisonChecklistItemVO titleItem = new SwapComparisonChecklistItemVO();
        titleItem.setFieldName("title");
        titleItem.setFieldLabel("标题");
        titleItem.setFromValue(fromItem != null ? fromItem.getTitle() : "");
        titleItem.setToValue(toItem != null ? toItem.getTitle() : "");
        titleItem.setIsDifferent(!safeEquals(fromItem != null ? fromItem.getTitle() : null,
                toItem != null ? toItem.getTitle() : null));
        titleItem.setIsImageField(false);
        checklist.add(titleItem);

        SwapComparisonChecklistItemVO descItem = new SwapComparisonChecklistItemVO();
        descItem.setFieldName("description");
        descItem.setFieldLabel("描述");
        descItem.setFromValue(fromItem != null ? fromItem.getDescription() : "");
        descItem.setToValue(toItem != null ? toItem.getDescription() : "");
        descItem.setIsDifferent(!safeEquals(fromItem != null ? fromItem.getDescription() : null,
                toItem != null ? toItem.getDescription() : null));
        descItem.setIsImageField(false);
        checklist.add(descItem);

        SwapComparisonChecklistItemVO conditionItem = new SwapComparisonChecklistItemVO();
        conditionItem.setFieldName("condition");
        conditionItem.setFieldLabel("成色");
        conditionItem.setFromValue(fromItem != null ? fromItem.getCondition() : "");
        conditionItem.setToValue(toItem != null ? toItem.getCondition() : "");
        conditionItem.setIsDifferent(!safeEquals(fromItem != null ? fromItem.getCondition() : null,
                toItem != null ? toItem.getCondition() : null));
        conditionItem.setIsImageField(false);
        checklist.add(conditionItem);

        SwapComparisonChecklistItemVO expectedItem = new SwapComparisonChecklistItemVO();
        expectedItem.setFieldName("expectedSwap");
        expectedItem.setFieldLabel("期望交换");
        expectedItem.setFromValue(fromItem != null ? fromItem.getExpectedSwap() : "");
        expectedItem.setToValue(toItem != null ? toItem.getExpectedSwap() : "");
        expectedItem.setIsDifferent(!safeEquals(fromItem != null ? fromItem.getExpectedSwap() : null,
                toItem != null ? toItem.getExpectedSwap() : null));
        expectedItem.setIsImageField(false);
        checklist.add(expectedItem);

        return checklist;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
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

    private void checkOfferExpired(SwapOffer offer) {
        if (offer == null || !SwapOfferStatus.PENDING.getCode().equals(offer.getStatus())) {
            return;
        }
        if (offer.getExpireTime() != null && offer.getExpireTime().isBefore(LocalDateTime.now())) {
            expireOffer(offer, EXPIRE_REASON_TIMEOUT);
            throw new RuntimeException("邀约已超时失效");
        }
    }

    @Transactional
    public void expireOffer(SwapOffer offer, String reason) {
        if (offer == null || !SwapOfferStatus.PENDING.getCode().equals(offer.getStatus())) {
            return;
        }

        String oldStatus = offer.getStatus();
        offer.setStatus(SwapOfferStatus.EXPIRED.getCode());
        offer.setExpireReason(reason);
        swapOfferMapper.updateById(offer);

        recordStatusLog(offer.getId(), oldStatus, SwapOfferStatus.EXPIRED.getCode(),
                SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME, reason);
        log.info("互换邀约[{}]已失效，原因: {}", offer.getId(), reason);

        Item fromItem = itemMapper.selectById(offer.getFromItemId());
        Item toItem = itemMapper.selectById(offer.getToItemId());

        notificationService.createOfferExpiredNotification(
                offer.getFromUserId(),
                offer.getId(),
                offer.getToItemId(),
                fromItem != null ? fromItem.getTitle() : "",
                toItem != null ? toItem.getTitle() : "",
                reason
        );
        notificationService.createOfferExpiredNotification(
                offer.getToUserId(),
                offer.getId(),
                offer.getToItemId(),
                fromItem != null ? fromItem.getTitle() : "",
                toItem != null ? toItem.getTitle() : "",
                reason
        );

        clearUserOfferCaches(offer.getFromUserId());
        clearUserOfferCaches(offer.getToUserId());
    }

    @Transactional
    public int processExpiredOffers() {
        List<SwapOffer> expiredOffers = swapOfferMapper.selectList(
                new LambdaQueryWrapper<SwapOffer>()
                        .eq(SwapOffer::getStatus, SwapOfferStatus.PENDING.getCode())
                        .isNotNull(SwapOffer::getExpireTime)
                        .lt(SwapOffer::getExpireTime, LocalDateTime.now())
        );

        if (expiredOffers.isEmpty()) {
            return 0;
        }

        log.info("发现 {} 条超时未处理的互换邀约，开始处理", expiredOffers.size());
        int count = 0;
        for (SwapOffer offer : expiredOffers) {
            try {
                expireOffer(offer, EXPIRE_REASON_TIMEOUT);
                count++;
            } catch (Exception e) {
                log.error("处理超时邀约失败，offerId: {}", offer.getId(), e);
            }
        }
        log.info("超时邀约处理完成，成功失效 {} 条", count);
        return count;
    }

    public boolean isOfferExpired(SwapOffer offer) {
        if (offer == null || offer.getExpireTime() == null) {
            return false;
        }
        return offer.getExpireTime().isBefore(LocalDateTime.now());
    }

    @Transactional
    public void startHandover(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }
        if (!offer.getFromUserId().equals(userId) && !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("无权操作该邀约");
        }

        validateStatusTransition(offer.getStatus(), SwapOfferStatus.HANDOVER.getCode());

        String oldStatus = offer.getStatus();
        offer.setStatus(SwapOfferStatus.HANDOVER.getCode());
        swapOfferMapper.updateById(offer);

        User operator = userMapper.selectById(userId);
        recordStatusLog(offer.getId(), oldStatus, SwapOfferStatus.HANDOVER.getCode(),
                userId, operator != null ? operator.getNickname() : null, "确认物品交接中");
        log.info("用户[{}]确认邀约[{}]进入交接状态，状态: {} -> {}",
                userId, offerId, oldStatus, SwapOfferStatus.HANDOVER.getCode());

        notificationService.createOfferHandoverNotification(
                offer.getFromUserId().equals(userId) ? offer.getToUserId() : offer.getFromUserId(),
                offer.getId(),
                offer.getToItemId(),
                offer.getFromItem() != null ? offer.getFromItem().getTitle() : "",
                offer.getToItem() != null ? offer.getToItem().getTitle() : ""
        );

        clearUserOfferCaches(userId);
        clearUserOfferCaches(offer.getFromUserId().equals(userId) ? offer.getToUserId() : offer.getFromUserId());
    }

    @Transactional
    public void completeOffer(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }
        if (!offer.getFromUserId().equals(userId) && !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("无权操作该邀约");
        }

        validateStatusTransition(offer.getStatus(), SwapOfferStatus.COMPLETED.getCode());

        String oldStatus = offer.getStatus();
        offer.setStatus(SwapOfferStatus.COMPLETED.getCode());
        swapOfferMapper.updateById(offer);

        User operator = userMapper.selectById(userId);
        recordStatusLog(offer.getId(), oldStatus, SwapOfferStatus.COMPLETED.getCode(),
                userId, operator != null ? operator.getNickname() : null, "确认物品交接完成");
        log.info("用户[{}]确认邀约[{}]完成，状态: {} -> {}",
                userId, offerId, oldStatus, SwapOfferStatus.COMPLETED.getCode());

        Long otherUserId = offer.getFromUserId().equals(userId) ? offer.getToUserId() : offer.getFromUserId();
        notificationService.createOfferCompletedNotification(
                otherUserId,
                offer.getId(),
                offer.getToItemId(),
                offer.getFromItem() != null ? offer.getFromItem().getTitle() : "",
                offer.getToItem() != null ? offer.getToItem().getTitle() : ""
        );

        clearUserOfferCaches(userId);
        clearUserOfferCaches(otherUserId);
    }

    public List<OfferTimelineNodeVO> getOfferTimeline(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null) {
            throw new RuntimeException("邀约不存在");
        }
        if (!offer.getFromUserId().equals(userId) && !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("无权查看该邀约");
        }

        List<SwapOfferStatusLog> statusLogs = swapOfferStatusLogMapper.selectList(
                new LambdaQueryWrapper<SwapOfferStatusLog>()
                        .eq(SwapOfferStatusLog::getOfferId, offerId)
                        .orderByAsc(SwapOfferStatusLog::getCreateTime)
        );

        Map<String, SwapOfferStatusLog> logMap = new HashMap<>();
        for (SwapOfferStatusLog log : statusLogs) {
            logMap.put(log.getToStatus(), log);
        }

        String currentStatus = offer.getStatus();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<OfferTimelineNodeVO> timeline = new ArrayList<>();

        SwapOfferStatusLog pendingLog = logMap.get(SwapOfferStatus.PENDING.getCode());
        String createTime = pendingLog != null && pendingLog.getCreateTime() != null
                ? pendingLog.getCreateTime().format(formatter)
                : (offer.getCreateTime() != null ? offer.getCreateTime().format(formatter) : "");
        String pendingOperator = pendingLog != null ? pendingLog.getOperatorName() : (offer.getFromUser() != null ? offer.getFromUser().getNickname() : "");
        String pendingRemark = pendingLog != null ? pendingLog.getRemark() : "发起互换邀约";
        timeline.add(new OfferTimelineNodeVO(
                SwapOfferStatus.PENDING.getCode(),
                "发起邀约",
                createTime,
                pendingOperator,
                pendingRemark,
                true,
                SwapOfferStatus.PENDING.getCode().equals(currentStatus),
                SwapOfferStatus.PENDING.getCode().equals(currentStatus) ? "#409eff" : "#67c23a",
                "Plus"
        ));

        if (SwapOfferStatus.REJECTED.getCode().equals(currentStatus)) {
            SwapOfferStatusLog rejectedLog = logMap.get(SwapOfferStatus.REJECTED.getCode());
            String rejectTime = rejectedLog != null && rejectedLog.getCreateTime() != null
                    ? rejectedLog.getCreateTime().format(formatter) : "";
            String rejectOperator = rejectedLog != null ? rejectedLog.getOperatorName() : "";
            String rejectRemark = rejectedLog != null ? rejectedLog.getRemark() : "驳回互换邀约";
            timeline.add(new OfferTimelineNodeVO(
                    SwapOfferStatus.REJECTED.getCode(),
                    "邀约已驳回",
                    rejectTime,
                    rejectOperator,
                    rejectRemark,
                    true,
                    true,
                    "#f56c6c",
                    "Close"
            ));
            return timeline;
        }

        if (SwapOfferStatus.EXPIRED.getCode().equals(currentStatus)) {
            SwapOfferStatusLog expiredLog = logMap.get(SwapOfferStatus.EXPIRED.getCode());
            String expireTime = expiredLog != null && expiredLog.getCreateTime() != null
                    ? expiredLog.getCreateTime().format(formatter) : "";
            String expireOperator = expiredLog != null ? expiredLog.getOperatorName() : "系统";
            String expireRemark = expiredLog != null ? expiredLog.getRemark() : "邀约已失效";
            timeline.add(new OfferTimelineNodeVO(
                    SwapOfferStatus.EXPIRED.getCode(),
                    "邀约已失效",
                    expireTime,
                    expireOperator,
                    expireRemark,
                    true,
                    true,
                    "#909399",
                    "Clock"
            ));
            return timeline;
        }

        boolean acceptedDone = !SwapOfferStatus.PENDING.getCode().equals(currentStatus);
        SwapOfferStatusLog acceptedLog = logMap.get(SwapOfferStatus.ACCEPTED.getCode());
        String acceptTime = acceptedLog != null && acceptedLog.getCreateTime() != null
                ? acceptedLog.getCreateTime().format(formatter) : "";
        String acceptOperator = acceptedLog != null ? acceptedLog.getOperatorName() : "";
        String acceptRemark = acceptedLog != null ? acceptedLog.getRemark() : "接受互换邀约";
        timeline.add(new OfferTimelineNodeVO(
                SwapOfferStatus.ACCEPTED.getCode(),
                "接受邀约",
                acceptTime,
                acceptOperator,
                acceptRemark,
                acceptedDone,
                SwapOfferStatus.ACCEPTED.getCode().equals(currentStatus),
                getNodeColor(currentStatus, SwapOfferStatus.ACCEPTED.getCode()),
                "Check"
        ));

        boolean handoverDone = SwapOfferStatus.HANDOVER.getCode().equals(currentStatus)
                || SwapOfferStatus.COMPLETED.getCode().equals(currentStatus);
        SwapOfferStatusLog handoverLog = logMap.get(SwapOfferStatus.HANDOVER.getCode());
        String handoverTime = handoverLog != null && handoverLog.getCreateTime() != null
                ? handoverLog.getCreateTime().format(formatter) : "";
        String handoverOperator = handoverLog != null ? handoverLog.getOperatorName() : "";
        String handoverRemark = handoverLog != null ? handoverLog.getRemark() : "物品交接中，请双方确认线下/快递交接";
        timeline.add(new OfferTimelineNodeVO(
                SwapOfferStatus.HANDOVER.getCode(),
                "物品交接",
                handoverTime,
                handoverOperator,
                handoverRemark,
                handoverDone,
                SwapOfferStatus.HANDOVER.getCode().equals(currentStatus),
                getNodeColor(currentStatus, SwapOfferStatus.HANDOVER.getCode()),
                "Van"
        ));

        boolean completedDone = SwapOfferStatus.COMPLETED.getCode().equals(currentStatus);
        SwapOfferStatusLog completedLog = logMap.get(SwapOfferStatus.COMPLETED.getCode());
        String completeTime = completedLog != null && completedLog.getCreateTime() != null
                ? completedLog.getCreateTime().format(formatter) : "";
        String completeOperator = completedLog != null ? completedLog.getOperatorName() : "";
        String completeRemark = completedLog != null ? completedLog.getRemark() : "物品交接完成，可进行相互评价";
        timeline.add(new OfferTimelineNodeVO(
                SwapOfferStatus.COMPLETED.getCode(),
                "交换完成",
                completeTime,
                completeOperator,
                completeRemark,
                completedDone,
                SwapOfferStatus.COMPLETED.getCode().equals(currentStatus),
                getNodeColor(currentStatus, SwapOfferStatus.COMPLETED.getCode()),
                "Finished"
        ));

        timeline.add(new OfferTimelineNodeVO(
                "reviewed",
                "完成评价",
                "",
                "",
                "双方互评后完成整个互换流程",
                false,
                false,
                "#c0c4cc",
                "Star"
        ));

        return timeline;
    }

    private String getNodeColor(String currentStatus, String nodeStatus) {
        if (nodeStatus.equals(currentStatus)) {
            return "#409eff";
        }
        List<String> order = Arrays.asList(
                SwapOfferStatus.PENDING.getCode(),
                SwapOfferStatus.ACCEPTED.getCode(),
                SwapOfferStatus.HANDOVER.getCode(),
                SwapOfferStatus.COMPLETED.getCode()
        );
        int currentIdx = order.indexOf(currentStatus);
        int nodeIdx = order.indexOf(nodeStatus);
        if (nodeIdx < currentIdx) {
            return "#67c23a";
        }
        return "#c0c4cc";
    }
}
