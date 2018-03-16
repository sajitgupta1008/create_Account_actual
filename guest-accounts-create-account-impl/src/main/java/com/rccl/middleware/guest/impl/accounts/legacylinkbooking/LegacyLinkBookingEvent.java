package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import lombok.Builder;
import lombok.Getter;
import org.codehaus.jackson.annotate.JsonCreator;

@Getter
@Builder
public final class LegacyLinkBookingEvent implements Jsonable, AggregateEvent<LegacyLinkBookingEvent> {
    
    @Override
    public AggregateEventTagger<LegacyLinkBookingEvent> aggregateTag() {
        return LegacyLinkBookingTag.INSTANCE;
    }
    
    private static final long serialVersionUID = 1L;
    
    private final LegacyLinkBookingMessage legacyLinkBookingEvent;
    
    @JsonCreator
    public LegacyLinkBookingEvent(LegacyLinkBookingMessage legacyLinkBookingEvent) {
        this.legacyLinkBookingEvent = legacyLinkBookingEvent;
    }
}
