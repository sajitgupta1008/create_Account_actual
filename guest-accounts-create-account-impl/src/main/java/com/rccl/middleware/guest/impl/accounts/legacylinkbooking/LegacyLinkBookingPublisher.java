package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import akka.japi.Pair;
import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;

import javax.inject.Inject;
import java.util.List;

public class LegacyLinkBookingPublisher {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(LegacyLinkBookingPublisher.class);
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    @Inject
    public LegacyLinkBookingPublisher(PersistentEntityRegistry persistentEntityRegistry) {
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(LegacyLinkBookingEntity.class);
    }
    
    public Topic<LegacyLinkBookingMessage> topic() {
        return TopicProducer.singleStreamWithOffset(offset ->
                persistentEntityRegistry
                        .eventStream(LegacyLinkBookingTag.INSTANCE, offset)
                        .map(pair -> {
                            LOGGER.info("Publishing a Legacy Link Booking event...");
                            
                            LegacyLinkBookingMessage event = pair.first().getLegacyLinkBookingEvent();
                            
                            String brand = event.getBrand();
                            List<String> consumerIds = event.getConsumerIds();
                            Guest guest = event.getGuest();
                            List<String> reservationUserIds = event.getReservationUserIds();
                            List<String> webshopperIds = event.getWebshopperIds();
                            
                            LegacyLinkBookingMessage message = LegacyLinkBookingMessage
                                    .builder()
                                    .brand(brand)
                                    .consumerIds(consumerIds)
                                    .guest(guest)
                                    .reservationUserIds(reservationUserIds)
                                    .webshopperIds(webshopperIds)
                                    .build();
                            
                            return new Pair<>(message, pair.second());
                        }));
    }
    
    public void publish(String brand,
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
