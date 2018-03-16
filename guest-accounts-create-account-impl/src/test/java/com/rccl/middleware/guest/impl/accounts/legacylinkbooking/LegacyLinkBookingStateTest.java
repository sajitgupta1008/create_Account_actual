package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.serialization.CompressedJsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LegacyLinkBookingStateTest {
    
    @Test
    public void testClassImplementsCompressedJsonable() {
        assertTrue(CompressedJsonable.class.isAssignableFrom(LegacyLinkBookingState.class));
    }
    
    @Test
    public void testEmptyState() {
        LegacyLinkBookingState emptyState = LegacyLinkBookingState.EMPTY_STATE;
        
        assertNull(emptyState.getMessage());
        assertNull(emptyState.getTimestamp());
    }
    
    @Test
    public void testConstructor() {
        Long expectedTimestamp = 42L;
        LegacyLinkBookingMessage expectedMessage = LegacyLinkBookingMessage.builder().build();
        
        LegacyLinkBookingState state = new LegacyLinkBookingState(expectedMessage, expectedTimestamp);
        
        assertEquals(expectedMessage, state.getMessage());
        assertEquals(expectedTimestamp, state.getTimestamp());
    }
}
