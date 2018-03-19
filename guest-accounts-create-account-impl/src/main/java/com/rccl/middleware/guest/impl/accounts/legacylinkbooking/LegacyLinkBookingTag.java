package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;

public class LegacyLinkBookingTag {
    
    private static final Class<LegacyLinkBookingEvent> EVENT_TYPE = LegacyLinkBookingEvent.class;
    
    private static final String TAG = LegacyLinkBookingEvent.class.getSimpleName();
    
    public static final AggregateEventTag<LegacyLinkBookingEvent> INSTANCE =
            AggregateEventTag.of(EVENT_TYPE, TAG);
}
