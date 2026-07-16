package com.zhifutong.customer.vo;

import com.zhifutong.customer.domain.TicketCategory;
import com.zhifutong.customer.domain.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketNo,
        Long conversationId,
        TicketCategory category,
        String description,
        String contact,
        TicketStatus status,
        String handlingNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
