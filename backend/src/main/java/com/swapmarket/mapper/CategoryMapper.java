package com.swapmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swapmarket.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
