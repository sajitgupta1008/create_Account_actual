package com.rccl.middleware.guest.accounts.legacylinkbooking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.rccl.middleware.guest.accounts.Guest;
import lombok.Getter;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = Void.class)
@JsonSubTypes({@JsonSubTypes.Type(LegacyLinkBookingEvent.LegacyAccountLinked.class)})
public interface LegacyLinkBookingEvent {
    
    @Getter
    @JsonTypeName("legacyAccountLinked")
    final class LegacyAccountLinked implements LegacyLinkBookingEvent {
        
        private final Guest guest;
        
        private final List<String> webshopperIds;
        
        private final List<String> reservationUserIds;
        
        private final String brand;
        
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public LegacyAccountLinked(
                @JsonProperty("guest") Guest guest,
                @JsonProperty("webshopperIds") List<String> webshopperIds,
                @JsonProperty("reservationUserIds") List<String> reservationUserIds,
                @JsonProperty("brand") String brand) {
            this.guest = guest;
            this.webshopperIds = webshopperIds;
            this.reservationUserIds = reservationUserIds;
            this.brand = brand;
        }
    }
}
