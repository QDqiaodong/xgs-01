package com.swapmarket.vo;

import lombok.Data;

@Data
public class SwapComparisonChecklistItemVO {

    private String fieldName;

    private String fieldLabel;

    private String fromValue;

    private String toValue;

    private Boolean isDifferent;

    private Boolean isImageField;
}
