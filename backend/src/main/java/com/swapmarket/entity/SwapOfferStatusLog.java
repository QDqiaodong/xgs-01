package com.swapmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("swap_offer_status_log")
public class SwapOfferStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long offerId;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static SwapOfferStatusLog of(Long offerId, String fromStatus, String toStatus,
                                         Long operatorId, String operatorName, String remark) {
        SwapOfferStatusLog log = new SwapOfferStatusLog();
        log.setOfferId(offerId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        return log;
    }
}
