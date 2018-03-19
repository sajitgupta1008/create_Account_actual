package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LegacyLinkBookingEntityTest {
    
    @Test
    public void testClassIsPersistentEntity() {
        assertTrue(PersistentEntity.class.isAssignableFrom(LegacyLinkBookingEntity.class));
    }
    
    @Test
    public void testInitialBehavior() {
        PersistentEntity.Behavior behavior = new LegacyLinkBookingEntity().initialBehavior(Optional.empty());
        
        // Test state.
        
        LegacyLinkBookingState state = (LegacyLinkBookingState) behavior.state();
        assertNotNull(state);
        assertEquals(LegacyLinkBookingState.EMPTY_STATE, state);
        
        // Test command handlers.
        
        scala.collection.immutable.Map commandHandlers = behavior.commandHandlers();
        assertNotNull(commandHandlers);
        
        @SuppressWarnings("unchecked")
        boolean containsCommand = commandHandlers.contains(LegacyLinkBookingCommand.class);
        assertTrue(containsCommand);
        
        // Test event handlers.
        
        scala.collection.immutable.Map eventHandlers = behavior.eventHandlers();
        assertNotNull(eventHandlers);
        
        @SuppressWarnings("unchecked")
        boolean containsEvent = eventHandlers.contains(LegacyLinkBookingEvent.class);
        assertTrue(containsEvent);
    }
    
    @Test
    public void testInitialBehaviorWithCustomState() {
        LegacyLinkBookingState customState = new LegacyLinkBookingState(null, 42L);
        PersistentEntity.Behavior behavior = new LegacyLinkBookingEntity().initialBehavior(Optional.of(customState));
        
        LegacyLinkBookingState state = (LegacyLinkBookingState) behavior.state();
        assertNotNull(state);
        
        assertNull(state.getMessage());
        assertEquals(state.getTimestamp(), customState.getTimestamp());
    }
}
