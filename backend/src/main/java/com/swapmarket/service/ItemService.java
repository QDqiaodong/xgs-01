package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.common.PageResult;
import com.swapmarket.entity.Category;
import com.swapmarket.entity.Favorite;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.ItemImage;
import com.swapmarket.entity.ItemLike;
import com.swapmarket.mapper.CategoryMapper;
import com.swapmarket.mapper.FavoriteMapper;
import com.swapmarket.mapper.ItemImageMapper;
import com.swapmarket.mapper.ItemLikeMapper;
import com.swapmarket.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final ItemLikeMapper itemLikeMapper;
    private final FileStorageService fileStorageService;
    private final RedisCacheService redisCacheService;
    private final ViewCountService viewCountService;

    public List<Item> getTopItems() {
        List<Item> cached = redisCacheService.getList(CacheKeyConstants.TOP_ITEMS_KEY, Item.class);
        if (cached != null) {
            return cached;
        }

        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "published")
                .eq(Item::getIsTop, 1)
                .orderByDesc(Item::getCreateTime)
                .last("LIMIT 8"));
        
        enrichItems(items);
        
        redisCacheService.set(CacheKeyConstants.TOP_ITEMS_KEY, items,
                CacheKeyConstants.TOP_ITEMS_TTL, CacheKeyConstants.TOP_ITEMS_TTL_UNIT);
        return items;
    }

    public PageResult<Item> listItems(int page, int size, Long categoryId, String condition,
                                      List<String> conditions, String timeRange, String keyword,
                                      Boolean hasImages, String exchangeKeyword) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "published")
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .eq(condition != null && conditions == null, Item::getCondition, condition)
                .in(conditions != null && !conditions.isEmpty(), Item::getCondition, conditions)
                .and(keyword != null, w -> w
                        .like(Item::getTitle, keyword)
                        .or()
                        .like(Item::getDescription, keyword))
                .like(exchangeKeyword != null, Item::getExpectedSwap, exchangeKeyword)
                .orderByDesc(Item::getCreateTime);

        if (timeRange != null) {
            LocalDateTime startTime = null;
            switch (timeRange) {
                case "today":
                    startTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                    break;
                case "week":
                    LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    startTime = LocalDateTime.of(monday, LocalTime.MIN);
                    break;
                case "month":
                    LocalDate firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
                    startTime = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN);
                    break;
            }
            if (startTime != null) {
                wrapper.ge(Item::getCreateTime, startTime);
            }
        }

        if (Boolean.TRUE.equals(hasImages)) {
            wrapper.exists("SELECT 1 FROM item_image ii WHERE ii.item_id = item.id");
        }

        Page<Item> itemPage = itemMapper.selectPage(Page.of(page, size), wrapper);
        enrichItems(itemPage.getRecords());

        return PageResult.of(itemPage.getRecords(), itemPage.getTotal(), page, size);
    }

    public List<Item> getMyItems(Long userId, String status) {
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getUserId, userId)
                .eq(status != null, Item::getStatus, status)
                .orderByDesc(Item::getCreateTime));
        enrichItems(items);
        return items;
    }

    public Item getDetail(Long id) {
        String cacheKey = CacheKeyConstants.ITEM_DETAIL_KEY + id;
        Item cached = redisCacheService.getObject(cacheKey, Item.class);
        if (cached != null) {
            return cached;
        }

        Item item = itemMapper.selectById(id);
        if (item != null) {
            enrichItems(List.of(item));
            redisCacheService.set(cacheKey, item,
                    CacheKeyConstants.ITEM_DETAIL_TTL, CacheKeyConstants.ITEM_DETAIL_TTL_UNIT);
        }
        return item;
    }

    @Transactional
    public Item publish(Long userId, String title, String description, Long categoryId,
                        String condition, String expectedSwap, List<MultipartFile> images) throws IOException {
        Item item = new Item();
        item.setUserId(userId);
        item.setTitle(title);
        item.setDescription(description);
        item.setCategoryId(categoryId);
        item.setCondition(condition);
        item.setExpectedSwap(expectedSwap);
        item.setStatus("published");
        item.setIsTop(0);
        item.setViewCount(0);
        item.setLikeCount(0);
        itemMapper.insert(item);

        if (images != null && !images.isEmpty()) {
            int sortOrder = 0;
            for (MultipartFile image : images) {
                String imageUrl = fileStorageService.saveFile(image);
                ItemImage itemImage = new ItemImage();
                itemImage.setItemId(item.getId());
                itemImage.setImageUrl(imageUrl);
                itemImage.setSortOrder(sortOrder++);
                itemImageMapper.insert(itemImage);
            }
        }

        clearItemRelatedCaches(item.getId());
        return item;
    }

    @Transactional
    public void offline(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item != null && item.getUserId().equals(userId)) {
            item.setStatus("offline");
            itemMapper.updateById(item);
            clearItemRelatedCaches(id);
        }
    }

    @Transactional
    public void republish(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item != null && item.getUserId().equals(userId)) {
            item.setStatus("published");
            itemMapper.updateById(item);
            clearItemRelatedCaches(id);
        }
    }

    private void enrichItems(List<Item> items) {
        enrichItems(items, null);
    }

    private void enrichItems(List<Item> items, Long userId) {
        if (items.isEmpty()) return;

        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        Map<Long, List<String>> imageMap = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .orderByAsc(ItemImage::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(
                        ItemImage::getItemId,
                        Collectors.mapping(ItemImage::getImageUrl, Collectors.toList())
                ));

        Set<Long> favoriteItemIds = null;
        Set<Long> likedItemIds = null;
        if (userId != null) {
            favoriteItemIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, userId)
                            .in(Favorite::getItemId, itemIds))
                    .stream()
                    .map(Favorite::getItemId)
                    .collect(Collectors.toSet());

            likedItemIds = itemLikeMapper.selectList(new LambdaQueryWrapper<ItemLike>()
                            .eq(ItemLike::getUserId, userId)
                            .in(ItemLike::getItemId, itemIds))
                    .stream()
                    .map(ItemLike::getItemId)
                    .collect(Collectors.toSet());
        }

        for (Item item : items) {
            item.setCategoryName(categoryMap.get(item.getCategoryId()));
            item.setImages(imageMap.getOrDefault(item.getId(), new ArrayList<>()));
            if (item.getLikeCount() == null) {
                item.setLikeCount(0);
            }
            if (userId != null) {
                item.setFavorited(favoriteItemIds.contains(item.getId()));
                item.setLiked(likedItemIds.contains(item.getId()));
            }
        }
    }

    @Transactional
    public void addFavorite(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        if (!"published".equals(item.getStatus())) {
            throw new RuntimeException("物品已下架，无法收藏");
        }
        if (item.getUserId().equals(userId)) {
            throw new RuntimeException("不能收藏自己发布的物品");
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId));
        if (existing != null) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favoriteMapper.insert(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId));
    }

    public List<Item> getMyFavorites(Long userId) {
        List<Long> favoriteItemIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime))
                .stream()
                .map(Favorite::getItemId)
                .collect(Collectors.toList());

        if (favoriteItemIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .in(Item::getId, favoriteItemIds)
                .eq(Item::getStatus, "published"));

        enrichItems(items, userId);

        Map<Long, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < favoriteItemIds.size(); i++) {
            orderMap.put(favoriteItemIds.get(i), i);
        }
        items.sort((a, b) -> {
            Integer oa = orderMap.get(a.getId());
            Integer ob = orderMap.get(b.getId());
            return oa.compareTo(ob);
        });

        return items;
    }

    public List<Item> getTopItems(Long userId) {
        List<Item> items = getTopItems();
        if (userId != null) {
            enrichItems(items, userId);
        }
        return items;
    }

    public PageResult<Item> listItems(int page, int size, Long categoryId, String condition,
                                      List<String> conditions, String timeRange, String keyword,
                                      Boolean hasImages, String exchangeKeyword, Long userId) {
        PageResult<Item> result = listItems(page, size, categoryId, condition, conditions, timeRange, keyword, hasImages, exchangeKeyword);
        if (userId != null) {
            enrichItems(result.getList(), userId);
        }
        return result;
    }

    public List<Item> getMyItems(Long userId, String status, Long currentUserId) {
        List<Item> items = getMyItems(userId, status);
        if (currentUserId != null) {
            enrichItems(items, currentUserId);
        }
        return items;
    }

    public Item getDetail(Long id, Long userId) {
        Item item = itemMapper.selectById(id);
        if (item != null) {
            enrichItems(List.of(item));
            if (userId != null) {
                enrichItems(List.of(item), userId);
            }
            viewCountService.recordView(id, userId);
        }
        return item;
    }

    @Transactional
    public void addLike(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        if (!"published".equals(item.getStatus())) {
            throw new RuntimeException("物品已下架，无法点赞");
        }
        ItemLike existing = itemLikeMapper.selectOne(new LambdaQueryWrapper<ItemLike>()
                .eq(ItemLike::getUserId, userId)
                .eq(ItemLike::getItemId, itemId));
        if (existing != null) {
            throw new RuntimeException("您已经点过赞了");
        }
        ItemLike itemLike = new ItemLike();
        itemLike.setUserId(userId);
        itemLike.setItemId(itemId);
        itemLikeMapper.insert(itemLike);

        item.setLikeCount(item.getLikeCount() == null ? 1 : item.getLikeCount() + 1);
        itemMapper.updateById(item);

        clearItemRelatedCaches(itemId);
    }

    @Transactional
    public void removeLike(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        ItemLike existing = itemLikeMapper.selectOne(new LambdaQueryWrapper<ItemLike>()
                .eq(ItemLike::getUserId, userId)
                .eq(ItemLike::getItemId, itemId));
        if (existing == null) {
            return;
        }
        itemLikeMapper.delete(new LambdaQueryWrapper<ItemLike>()
                .eq(ItemLike::getUserId, userId)
                .eq(ItemLike::getItemId, itemId));

        if (item.getLikeCount() != null && item.getLikeCount() > 0) {
            item.setLikeCount(item.getLikeCount() - 1);
            itemMapper.updateById(item);
        }

        clearItemRelatedCaches(itemId);
    }

    public List<Item> getLikeRanking(int limit, Long userId) {
        String cacheKey = CacheKeyConstants.LIKE_RANKING_KEY + ":" + limit;
        List<Item> cached = redisCacheService.getList(cacheKey, Item.class);
        if (cached != null) {
            if (userId != null) {
                enrichItems(cached, userId);
            }
            return cached;
        }

        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "published")
                .orderByDesc(Item::getLikeCount)
                .orderByDesc(Item::getCreateTime)
                .last("LIMIT " + Math.min(limit, 100)));

        enrichItems(items);

        redisCacheService.set(cacheKey, items,
                CacheKeyConstants.LIKE_RANKING_TTL, CacheKeyConstants.LIKE_RANKING_TTL_UNIT);

        if (userId != null) {
            enrichItems(items, userId);
        }
        return items;
    }

    private void clearItemRelatedCaches(Long itemId) {
        if (itemId != null) {
            redisCacheService.delete(CacheKeyConstants.ITEM_DETAIL_KEY + itemId);
        }
        redisCacheService.delete(CacheKeyConstants.TOP_ITEMS_KEY);
        redisCacheService.deleteByPattern(CacheKeyConstants.LIKE_RANKING_KEY + "*");
    }
}
