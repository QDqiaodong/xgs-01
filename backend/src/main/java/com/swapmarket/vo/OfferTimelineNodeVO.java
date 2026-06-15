package com.swapmarket.vo;

import lombok.Data;

@Data
public class OfferTimelineNodeVO {
    private String status;
    private String statusText;
    private String time;
    private String operator;
    private String remark;
    private Boolean done;
    private Boolean current;
    private String color;
    private String icon;

    public OfferTimelineNodeVO() {}

    public OfferTimelineNodeVO(String status, String statusText, String time, String operator, String remark, Boolean done, Boolean current, String color, String icon) {
        this.status = status;
        this.statusText = statusText;
        this.time = time;
        this.operator = operator;
        this.remark = remark;
        this.done = done;
        this.current = current;
        this.color = color;
        this.icon = icon;
    }
}
