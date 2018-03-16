package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LegacyLinkBookingCommandTest {
    
    @Test
    public void testConstructor() {
        LegacyLinkBookingCommand cmd = new LegacyLinkBookingCommand(null);
        assertNull(cmd.getLegacyLinkBookingMessage());
        
        LegacyLinkBookingMessage msg = LegacyLinkBookingMessage.builder().build();
        cmd = new LegacyLinkBookingCommand(msg);
        assertEquals(msg, cmd.getLegacyLinkBookingMessage());
    }
}
