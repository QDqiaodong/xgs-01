package com.swapmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("swap_offer")
public class SwapOffer {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long fromUserId;
    
    private Long toUserId;
    
    private Long fromItemId;
    
    private Long toItemId;
    
    private String message;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Item fromItem;

    @TableField(exist = false)
    private Item toItem;

    @TableField(exist = false)
    private User fromUser;
}
