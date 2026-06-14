package com.swapmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swapmarket.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    @Update("UPDATE item SET view_count = view_count + #{increment} WHERE id = #{itemId} AND deleted = 0")
    int updateViewCount(@Param("itemId") Long itemId, @Param("increment") int increment);

    @Select("<script>" +
            "SELECT i.* FROM item i " +
            "LEFT JOIN user u ON i.user_id = u.id " +
            "WHERE i.status = 'published' AND i.deleted = 0 " +
            "<if test='categoryId != null'>AND i.category_id = #{categoryId} </if>" +
            "<if test='condition != null'>AND i.condition = #{condition} </if>" +
            "<if test='conditions != null and conditions.size() > 0'>" +
            "AND i.condition IN <foreach collection='conditions' item='c' open='(' separator=',' close=')'>#{c}</foreach> " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (i.title LIKE CONCAT('%', #{keyword}, '%') OR i.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='exchangeKeyword != null and exchangeKeyword != \"\"'>" +
            "AND i.expected_swap LIKE CONCAT('%', #{exchangeKeyword}, '%') " +
            "</if>" +
            "<if test='startTime != null'>AND i.create_time &gt;= #{startTime} </if>" +
            "<if test='hasImages != null and hasImages'>" +
            "AND EXISTS (SELECT 1 FROM item_image ii WHERE ii.item_id = i.id) " +
            "</if>" +
            "ORDER BY " +
            "(CASE WHEN u.credit_score IS NOT NULL THEN u.credit_score ELSE 5.0 END) DESC, " +
            "i.create_time DESC " +
            "</script>")
    IPage<Item> selectPageWithCreditScore(Page<Item> page,
                                           @Param("categoryId") Long categoryId,
                                           @Param("condition") String condition,
                                           @Param("conditions") List<String> conditions,
                                           @Param("keyword") String keyword,
                                           @Param("exchangeKeyword") String exchangeKeyword,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("hasImages") Boolean hasImages);
}
