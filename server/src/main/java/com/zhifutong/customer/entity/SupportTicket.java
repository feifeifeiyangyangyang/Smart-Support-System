package com.zhifutong.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhifutong.customer.domain.TicketCategory;
import com.zhifutong.customer.domain.TicketStatus;
import java.time.LocalDateTime;

@TableName("support_ticket")
public class SupportTicket {
    private Long id;
    private String ticketNo;
    private Long conversationId;
    private TicketCategory category;
    private String description;
    private String contact;
    private TicketStatus status;
    private String handlingNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getHandlingNote() { return handlingNote; }
    public void setHandlingNote(String handlingNote) { this.handlingNote = handlingNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
