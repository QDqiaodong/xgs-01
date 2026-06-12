package com.swapmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swapmarket.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    @Update("UPDATE item SET view_count = view_count + #{increment} WHERE id = #{itemId} AND deleted = 0")
    int updateViewCount(@Param("itemId") Long itemId, @Param("increment") int increment);
}
