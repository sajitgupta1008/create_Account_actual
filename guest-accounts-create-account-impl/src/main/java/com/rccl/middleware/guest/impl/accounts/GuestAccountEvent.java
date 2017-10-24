package com.rccl.middleware.guest.impl.accounts;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonCreator;

public interface GuestAccountEvent extends Jsonable, AggregateEvent<GuestAccountEvent> {
    
    @Override
    default AggregateEventTagger<GuestAccountEvent> aggregateTag() {
        return GuestAccountTag.GUEST_ACCOUNT_EVENT_TAG;
    }
    
    @Value
    final class GuestCreated implements GuestAccountEvent {
        
        public final Guest guest;
        
        @JsonCreator
        public GuestCreated(Guest guest) {
            this.guest = guest;
        }
    }
    
    @Value
    final class GuestUpdated implements GuestAccountEvent {
        
        public final EnrichedGuest enrichedGuest;
        
        @JsonCreator
        public GuestUpdated(EnrichedGuest enrichedGuest) {
            this.enrichedGuest = enrichedGuest;
        }
    }
    
    @Value
    final class VerifyLoyalty implements GuestAccountEvent {
        
        public final EnrichedGuest enrichedGuest;
        
        @JsonCreator
        public VerifyLoyalty(EnrichedGuest enrichedGuest) {
            this.enrichedGuest = enrichedGuest;
        }
    }
}
