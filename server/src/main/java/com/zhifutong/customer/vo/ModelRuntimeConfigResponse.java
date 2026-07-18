package com.zhifutong.customer.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelRuntimeConfigResponse(
        BigDecimal temperature,
        Integer topK,
        Boolean mockEnabled,
        LocalDateTime updatedAt
) {
}
