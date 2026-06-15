package com.swapmarket.vo;

import lombok.Data;
import java.util.List;

@Data
public class SwapComparisonVO {

    private Long offerId;

    private String offerStatus;

    private String offerMessage;

    private String createTime;

    private SwapComparisonItemVO fromItem;

    private SwapComparisonItemVO toItem;

    private List<SwapComparisonChecklistItemVO> checklist;

    private Integer differentCount;

    private Integer totalCount;

    private String fromUserNickname;

    private String toUserNickname;
}
