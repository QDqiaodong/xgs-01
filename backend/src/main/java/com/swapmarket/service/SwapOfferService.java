package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.SwapOffer;
import com.swapmarket.entity.User;
import com.swapmarket.mapper.ItemMapper;
import com.swapmarket.mapper.SwapOfferMapper;
import com.swapmarket.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SwapOfferService {
    private final SwapOfferMapper swapOfferMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PENDING_OFFERS_KEY = "swap:offers:pending:";

    @Transactional
    public SwapOffer createOffer(Long fromUserId, Long fromItemId, Long toItemId, String message) {
        Item fromItem = itemMapper.selectById(fromItemId);
        Item toItem = itemMapper.selectById(toItemId);

        if (fromItem == null || toItem == null) {
            throw new RuntimeException("物品不存在");
        }

        SwapOffer offer = new SwapOffer();
        offer.setFromUserId(fromUserId);
        offer.setToUserId(toItem.getUserId());
        offer.setFromItemId(fromItemId);
        offer.setToItemId(toItemId);
        offer.setMessage(message);
        offer.setStatus("pending");
        swapOfferMapper.insert(offer);

        redisTemplate.delete(PENDING_OFFERS_KEY + toItem.getUserId());
        return offer;
    }

    public List<SwapOffer> getReceivedOffers(Long userId) {
        String key = PENDING_OFFERS_KEY + userId;
        List<SwapOffer> cached = (List<SwapOffer>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        List<SwapOffer> offers = swapOfferMapper.selectList(new LambdaQueryWrapper<SwapOffer>()
                .eq(SwapOffer::getToUserId, userId)
                .orderByDesc(SwapOffer::getCreateTime));
        
        redisTemplate.opsForValue().set(key, offers, 30, TimeUnit.MINUTES);
        return offers;
    }

    public List<SwapOffer> getSentOffers(Long userId) {
        return swapOfferMapper.selectList(new LambdaQueryWrapper<SwapOffer>()
                .eq(SwapOffer::getFromUserId, userId)
                .orderByDesc(SwapOffer::getCreateTime));
    }

    @Transactional
    public void acceptOffer(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null || !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("邀约不存在或无权操作");
        }
        offer.setStatus("accepted");
        swapOfferMapper.updateById(offer);

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

        redisTemplate.delete(PENDING_OFFERS_KEY + userId);
    }

    @Transactional
    public void rejectOffer(Long userId, Long offerId) {
        SwapOffer offer = swapOfferMapper.selectById(offerId);
        if (offer == null || !offer.getToUserId().equals(userId)) {
            throw new RuntimeException("邀约不存在或无权操作");
        }
        offer.setStatus("rejected");
        swapOfferMapper.updateById(offer);
        
        redisTemplate.delete(PENDING_OFFERS_KEY + userId);
    }
}
