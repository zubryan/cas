package org.apereo.cas.ticket.registry.queue;

import lombok.val;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.StringBean;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.support.NeverExpiresExpirationPolicy;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This is {@link UpdateTicketMessageQueueCommandTests}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class UpdateTicketMessageQueueCommandTests extends AbstractTicketMessageQueueCommandTests {
    
    @Test
    public void verifyUpdateTicket() {
        TicketGrantingTicket ticket = new TicketGrantingTicketImpl("TGT", CoreAuthenticationTestUtils.getAuthentication(),
                new NeverExpiresExpirationPolicy());
        val cmd = new UpdateTicketMessageQueueCommand(new StringBean(), ticket);
        cmd.execute(ticketRegistry);
        ticket = ticketRegistry.getTicket(ticket.getId(), ticket.getClass());
        assertNotNull(ticket);
        assertEquals("TGT", ticket.getId());
    }
}
