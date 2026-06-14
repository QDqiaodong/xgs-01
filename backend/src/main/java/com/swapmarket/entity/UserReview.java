package com.swapmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_review")
public class UserReview {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long offerId;

    private Long reviewerId;

    private Long targetUserId;

    private Integer rating;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private User reviewer;

    @TableField(exist = false)
    private Item fromItem;

    @TableField(exist = false)
    private Item toItem;
}
