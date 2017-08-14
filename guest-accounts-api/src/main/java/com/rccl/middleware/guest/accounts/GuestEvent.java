package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import lombok.Value;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = Void.class)
@JsonSubTypes({
        @JsonSubTypes.Type(GuestEvent.AccountCreated.class),
        @JsonSubTypes.Type(GuestEvent.AccountUpdated.class)
})
public interface GuestEvent {
    
    @Value
    @JsonTypeName("created")
    final class AccountCreated implements GuestEvent {
        private final Guest guest;
        
        @JsonCreator
        public AccountCreated(Guest guest) {
            this.guest = guest;
        }
    }
    
    @Value
    @JsonTypeName("updated")
    final class AccountUpdated implements GuestEvent {
        private final EnrichedGuest guest;
        
        @JsonCreator
        public AccountUpdated(EnrichedGuest guest) {
            this.guest = guest;
        }
    }
}
