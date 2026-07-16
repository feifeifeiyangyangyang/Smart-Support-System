package com.zhifutong.customer.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhifutong.customer.domain.TicketCategory;
import com.zhifutong.customer.domain.TicketStatus;
import com.zhifutong.customer.entity.SupportTicket;
import com.zhifutong.customer.exception.BusinessException;
import com.zhifutong.customer.mapper.SupportTicketMapper;
import com.zhifutong.customer.vo.PageResult;
import com.zhifutong.customer.vo.TicketResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketApplicationService {
    private final SupportTicketMapper ticketMapper;
    private final ConversationService conversationService;

    public TicketApplicationService(SupportTicketMapper ticketMapper, ConversationService conversationService) {
        this.ticketMapper = ticketMapper;
        this.conversationService = conversationService;
    }

    public TicketResponse create(Long conversationId, String description, TicketCategory category, String contact) {
        conversationService.require(conversationId);
        LocalDateTime now = LocalDateTime.now();
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("T" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        ticket.setConversationId(conversationId);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setContact(contact);
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticketMapper.insert(ticket);
        return toResponse(ticket);
    }

    public PageResult<TicketResponse> list(long page, long size, TicketStatus status) {
        Page<SupportTicket> result = ticketMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<SupportTicket>()
                        .eq(status != null, SupportTicket::getStatus, status)
                        .orderByDesc(SupportTicket::getCreatedAt));
        return new PageResult<>(page, size, result.getTotal(), result.getRecords().stream().map(this::toResponse).toList());
    }

    public TicketResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, TicketStatus next, String note) {
        SupportTicket ticket = require(id);
        if (!ticket.getStatus().canTransitTo(next)) {
            throw new BusinessException("工单状态不允许从 " + ticket.getStatus() + " 变更为 " + next);
        }
        ticket.setStatus(next);
        ticket.setHandlingNote(note);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        return toResponse(ticket);
    }

    private SupportTicket require(Long id) {
        SupportTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException("工单不存在");
        }
        return ticket;
    }

    private TicketResponse toResponse(SupportTicket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTicketNo(), ticket.getConversationId(), ticket.getCategory(),
                ticket.getDescription(), ticket.getContact(), ticket.getStatus(), ticket.getHandlingNote(),
                ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}
