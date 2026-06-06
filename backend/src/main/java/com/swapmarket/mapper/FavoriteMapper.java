package com.swapmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swapmarket.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
