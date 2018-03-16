package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyLinkBookingCommandTest {
    
    @Test
    public void testClassImplementsCompressedJsonable() {
        assertTrue(CompressedJsonable.class.isAssignableFrom(LegacyLinkBookingCommand.class));
    }
    
    @Test
    public void testClassImplementsPersistentEntityReplyType() {
        assertTrue(PersistentEntity.ReplyType.class.isAssignableFrom(LegacyLinkBookingCommand.class));
    }
    
    @Test
    public void testConstructor() {
        LegacyLinkBookingMessage msg = LegacyLinkBookingMessage.builder().build();
        
        LegacyLinkBookingCommand cmd = new LegacyLinkBookingCommand(msg);
        assertEquals(msg, cmd.getLegacyLinkBookingMessage());
    }
}
