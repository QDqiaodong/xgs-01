package com.swapmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("item")
public class Item {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String description;
    
    private Long categoryId;
    
    @TableField("`condition`")
    private String condition;
    
    private String expectedSwap;
    
    private String status;
    
    private Integer isTop;
    
    private Integer viewCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    @TableField(exist = false)
    private List<String> images;
    
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Boolean favorited;
}
