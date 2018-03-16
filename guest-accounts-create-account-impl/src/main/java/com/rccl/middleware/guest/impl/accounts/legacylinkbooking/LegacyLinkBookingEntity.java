package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;

import java.time.LocalDateTime;
import java.util.Optional;

public class LegacyLinkBookingEntity extends
        PersistentEntity<LegacyLinkBookingCommand, LegacyLinkBookingEvent, LegacyLinkBookingState> {
    
    @Override
    @SuppressWarnings("unchecked")
    public Behavior initialBehavior(Optional<LegacyLinkBookingState> snapshotState) {
        
        BehaviorBuilder builder = newBehaviorBuilder(snapshotState.orElse(LegacyLinkBookingState.EMPTY_STATE));
        
        builder.setCommandHandler(LegacyLinkBookingCommand.class, (cmd, commandContext) -> {
            LegacyLinkBookingMessage evt = cmd.getLegacyLinkBookingEvent();
            LegacyLinkBookingMessage message = LegacyLinkBookingMessage
                    .builder()
                    .brand(evt.getBrand())
                    .consumerIds(evt.getConsumerIds())
                    .guest(evt.getGuest())
                    .reservationUserIds(evt.getReservationUserIds())
                    .webshopperIds(evt.getWebshopperIds())
                    .build();
            
            return commandContext.thenPersist(new LegacyLinkBookingEvent(message),
                    event -> commandContext.reply(Done.getInstance()));
        });
        
        builder.setEventHandler(LegacyLinkBookingEvent.class,
                evt -> new LegacyLinkBookingState(evt.getLegacyLinkBookingEvent(), LocalDateTime.now().toString()));
        
        return builder.build();
    }
}
