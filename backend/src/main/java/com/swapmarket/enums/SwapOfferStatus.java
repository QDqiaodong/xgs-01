package com.swapmarket.enums;

import lombok.Getter;

@Getter
public enum SwapOfferStatus {
    PENDING("pending", "待处理"),
    ACCEPTED("accepted", "已接受"),
    REJECTED("rejected", "已驳回"),
    EXPIRED("expired", "已失效"),
    HANDOVER("handover", "交接中"),
    COMPLETED("completed", "已完成");

    private final String code;
    private final String description;

    SwapOfferStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SwapOfferStatus fromCode(String code) {
        for (SwapOfferStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean canTransitionTo(SwapOfferStatus targetStatus) {
        switch (this) {
            case PENDING:
                return targetStatus == ACCEPTED || targetStatus == REJECTED || targetStatus == EXPIRED;
            case ACCEPTED:
                return targetStatus == HANDOVER;
            case HANDOVER:
                return targetStatus == COMPLETED;
            default:
                return false;
        }
    }
}
