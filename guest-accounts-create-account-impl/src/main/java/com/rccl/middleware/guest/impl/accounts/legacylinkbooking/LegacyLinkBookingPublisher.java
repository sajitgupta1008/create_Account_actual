package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;

import javax.inject.Inject;
import java.util.List;

public class LegacyLinkBookingPublisher {
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    @Inject
    public LegacyLinkBookingPublisher(PersistentEntityRegistry persistentEntityRegistry) {
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(LegacyLinkBookingEntity.class);
    }
    
    public void publishLinkLegacyAccountEvent(String brand,
                                              List<String> consumerIds,
                                              Guest guest,
                                              List<String> reservationUserIds,
                                              List<String> webshopperIds) {
        
        LegacyLinkBookingMessage event = LegacyLinkBookingMessage
                .builder()
                .brand(brand)
                .consumerIds(consumerIds)
                .guest(guest)
                .reservationUserIds(reservationUserIds)
                .webshopperIds(webshopperIds)
                .build();
        
        String email = guest.getEmail();
        
        persistentEntityRegistry.refFor(LegacyLinkBookingEntity.class, email)
                .ask(new LegacyLinkBookingCommand(event));
    }
}
