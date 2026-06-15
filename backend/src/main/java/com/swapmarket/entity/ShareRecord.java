package com.swapmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share_record")
public class ShareRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long itemId;
    
    private Long sharerUserId;
    
    private Long visitorUserId;
    
    private String shareChannel;
    
    private String shareType;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
