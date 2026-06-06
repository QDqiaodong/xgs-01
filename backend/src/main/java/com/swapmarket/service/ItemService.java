package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swapmarket.common.PageResult;
import com.swapmarket.entity.Category;
import com.swapmarket.entity.Item;
import com.swapmarket.entity.ItemImage;
import com.swapmarket.mapper.CategoryMapper;
import com.swapmarket.mapper.ItemImageMapper;
import com.swapmarket.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final CategoryMapper categoryMapper;
    private final FileStorageService fileStorageService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOP_ITEMS_KEY = "swap:items:top";

    public List<Item> getTopItems() {
        List<Item> cached = (List<Item>) redisTemplate.opsForValue().get(TOP_ITEMS_KEY);
        if (cached != null) {
            return cached;
        }

        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "published")
                .eq(Item::getIsTop, 1)
                .orderByDesc(Item::getCreateTime)
                .last("LIMIT 8"));
        
        enrichItems(items);
        
        redisTemplate.opsForValue().set(TOP_ITEMS_KEY, items, 1, TimeUnit.HOURS);
        return items;
    }

    public PageResult<Item> listItems(int page, int size, Long categoryId, String condition, String keyword) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, "published")
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .eq(condition != null, Item::getCondition, condition)
                .and(keyword != null, w -> w
                        .like(Item::getTitle, keyword)
                        .or()
                        .like(Item::getDescription, keyword))
                .orderByDesc(Item::getCreateTime);

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
        Item item = itemMapper.selectById(id);
        if (item != null) {
            enrichItems(List.of(item));
            
            item.setViewCount(item.getViewCount() + 1);
            itemMapper.updateById(item);
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

        redisTemplate.delete(TOP_ITEMS_KEY);
        return item;
    }

    @Transactional
    public void offline(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item != null && item.getUserId().equals(userId)) {
            item.setStatus("offline");
            itemMapper.updateById(item);
            redisTemplate.delete(TOP_ITEMS_KEY);
        }
    }

    @Transactional
    public void republish(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item != null && item.getUserId().equals(userId)) {
            item.setStatus("published");
            itemMapper.updateById(item);
            redisTemplate.delete(TOP_ITEMS_KEY);
        }
    }

    private void enrichItems(List<Item> items) {
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

        for (Item item : items) {
            item.setCategoryName(categoryMap.get(item.getCategoryId()));
            item.setImages(imageMap.getOrDefault(item.getId(), new ArrayList<>()));
        }
    }
}
