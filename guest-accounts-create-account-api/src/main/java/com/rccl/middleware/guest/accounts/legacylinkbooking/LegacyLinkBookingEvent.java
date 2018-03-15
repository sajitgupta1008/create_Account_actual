package com.rccl.middleware.guest.accounts.legacylinkbooking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.rccl.middleware.guest.accounts.Guest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = Void.class)
@JsonSubTypes({@JsonSubTypes.Type(LegacyLinkBookingEvent.LegacyAccountLinked.class)})
public interface LegacyLinkBookingEvent {
    
    @Getter
    @Builder
    @JsonTypeName("legacyLink")
    final class LegacyAccountLinked implements LegacyLinkBookingEvent {
        
        private final Guest guest;
        
        private final List<String> webshopperIds;
        
        private final List<String> reservationUserIds;
        
        private final String brand;
        
        @JsonCreator
        public LegacyAccountLinked(Guest guest,
                                   List<String> webshopperIds,
                                   List<String> reservationUserIds,
                                   String brand) {
            this.guest = guest;
            this.webshopperIds = webshopperIds;
            this.reservationUserIds = reservationUserIds;
            this.brand = brand;
        }
    }
}
