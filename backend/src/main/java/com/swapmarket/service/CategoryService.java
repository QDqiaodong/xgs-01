package com.swapmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swapmarket.common.CacheKeyConstants;
import com.swapmarket.entity.Category;
import com.swapmarket.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final RedisCacheService redisCacheService;

    public List<Category> list() {
        List<Category> cached = redisCacheService.getList(CacheKeyConstants.CATEGORY_LIST_KEY, Category.class);
        if (cached != null) {
            return cached;
        }

        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder));

        redisCacheService.set(CacheKeyConstants.CATEGORY_LIST_KEY, categories,
                CacheKeyConstants.CATEGORY_LIST_TTL, CacheKeyConstants.CATEGORY_LIST_TTL_UNIT);
        return categories;
    }

    public void clearCategoryCache() {
        redisCacheService.delete(CacheKeyConstants.CATEGORY_LIST_KEY);
    }
}
