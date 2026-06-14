package com.swapmarket.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException invalidStatusTransition(String currentStatus, String targetStatus) {
        return new BusinessException(400,
                String.format("邀约状态非法变更：当前状态[%s]不允许变更为[%s]，只有待处理状态的邀约可被接受或驳回",
                        currentStatus, targetStatus));
    }
}
