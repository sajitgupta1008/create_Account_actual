package com.rccl.middleware.guest.accounts.legacylinkbooking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rccl.middleware.guest.accounts.Guest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LegacyLinkBookingMessage {
    
    private final String brand;
    
    private final List<String> consumerIds;
    
    private final Guest guest;
    
    private final List<String> reservationUserIds;
    
    private final List<String> webshopperIds;
    
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LegacyLinkBookingMessage(
            @JsonProperty("brand") String brand,
            @JsonProperty("consumerIds") List<String> consumerIds,
            @JsonProperty("guest") Guest guest,
            @JsonProperty("reservationUserIds") List<String> reservationUserIds,
            @JsonProperty("webshopperIds") List<String> webshopperIds) {
        this.brand = brand;
        this.consumerIds = consumerIds;
        this.guest = guest;
        this.reservationUserIds = reservationUserIds;
        this.webshopperIds = webshopperIds;
    }
}
