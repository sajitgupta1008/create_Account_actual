package com.rccl.middleware.guest.impl.accounts;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.Guest;
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
        public final Guest guest;
        
        @JsonCreator
        public GuestUpdated(Guest guest) {
            this.guest = guest;
        }
    }
}
