package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;

import java.util.Optional;

public class LegacyLinkBookingEntity extends
        PersistentEntity<LegacyLinkBookingCommand, LegacyLinkBookingEvent, LegacyLinkBookingState> {
    
    @Override
    @SuppressWarnings("unchecked")
    public Behavior initialBehavior(Optional<LegacyLinkBookingState> snapshotState) {
        BehaviorBuilder builder = this.newBehaviorBuilder(snapshotState.orElse(LegacyLinkBookingState.EMPTY_STATE));
        
        builder.setCommandHandler(LegacyLinkBookingCommand.class, (cmd, commandContext) -> {
            LegacyLinkBookingMessage msg = cmd.getLegacyLinkBookingMessage();
            LegacyLinkBookingEvent evt = new LegacyLinkBookingEvent(msg);
            return commandContext.thenPersist(evt, event -> commandContext.reply(Done.getInstance()));
        });
        
        builder.setEventHandler(LegacyLinkBookingEvent.class, evt -> {
            Long timestamp = System.currentTimeMillis();
            return new LegacyLinkBookingState(evt.getLegacyLinkBookingEvent(), timestamp);
        });
        
        return builder.build();
    }
}
