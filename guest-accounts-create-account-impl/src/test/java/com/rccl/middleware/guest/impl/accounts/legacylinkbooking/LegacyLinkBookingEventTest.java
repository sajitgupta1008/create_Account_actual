package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyLinkBookingEventTest {
    
    @Test
    public void testClassImplementsJsonable() {
        assertTrue(Jsonable.class.isAssignableFrom(LegacyLinkBookingEvent.class));
    }
    
    @Test
    public void testImplementsAggregateEvent() {
        assertTrue(AggregateEvent.class.isAssignableFrom(LegacyLinkBookingEvent.class));
    }
    
    @Test
    public void testAggregateTag() {
        assertEquals(LegacyLinkBookingTag.INSTANCE, new LegacyLinkBookingEvent(null).aggregateTag());
    }
    
    @Test
    public void testConstructor() {
        LegacyLinkBookingMessage expectedMessage = LegacyLinkBookingMessage.builder().build();
        LegacyLinkBookingEvent event = new LegacyLinkBookingEvent(expectedMessage);
        assertEquals(expectedMessage, event.getLegacyLinkBookingEvent());
    }
}
