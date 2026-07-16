package com.zhifutong.customer.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicketStatusTest {
    @Test
    void validatesTicketTransitions() {
        assertTrue(TicketStatus.PENDING.canTransitTo(TicketStatus.PROCESSING));
        assertTrue(TicketStatus.PROCESSING.canTransitTo(TicketStatus.RESOLVED));
        assertFalse(TicketStatus.CLOSED.canTransitTo(TicketStatus.PROCESSING));
    }
}
