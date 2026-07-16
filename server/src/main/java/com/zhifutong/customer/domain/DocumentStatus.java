package com.zhifutong.customer.domain;

public enum DocumentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean canRetry() {
        return this == FAILED;
    }
}
