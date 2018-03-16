package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LegacyLinkBookingTagTest {
    
    @Test
    public void testInstance() {
        AggregateEventTag aet = LegacyLinkBookingTag.INSTANCE;
        
        assertEquals(LegacyLinkBookingEvent.class, aet.eventType());
        assertEquals(LegacyLinkBookingEvent.class.getSimpleName(), aet.tag());
    }
}
