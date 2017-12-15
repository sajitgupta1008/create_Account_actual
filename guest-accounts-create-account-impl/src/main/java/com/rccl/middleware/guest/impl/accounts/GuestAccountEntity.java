package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.rccl.middleware.common.logging.RcclLoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuestAccountEntity extends PersistentEntity<GuestAccountCommand, GuestAccountEvent, GuestAccountState> {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(GuestAccountEntity.class);
    
    @Override
    @SuppressWarnings("unchecked")
    public Behavior initialBehavior(Optional<GuestAccountState> snapshotState) {
        BehaviorBuilder builder = newBehaviorBuilder(snapshotState.orElse(GuestAccountState.emptyState()));
        
        builder.setCommandHandler(GuestAccountCommand.CreateGuest.class,
                (cmd, ctx) -> {
                    LOGGER.debug("Adding GuestAccountEvent.GuestCreated event handler.");
                    return ctx.thenPersist(new GuestAccountEvent.GuestCreated(cmd.getGuest()),
                            evt -> ctx.reply(Done.getInstance()));
                }
        );
        
        builder.setEventHandler(GuestAccountEvent.GuestCreated.class,
                evt -> {
                    LOGGER.debug("Executing GuestAccountEvent.GuestCreated handler.");
                    return new GuestAccountState(evt.getGuest(), null,
                            LocalDateTime.now().toString(), GuestEventStatus.CREATE);
                });
        
        builder.setCommandHandler(GuestAccountCommand.UpdateGuest.class, (cmd, ctx) -> {
            if (cmd.getEnrichedGuest() == null) {
                ctx.invalidCommand("Guest cannot be null.");
                return ctx.done();
            }
            
            List<GuestAccountEvent> events = new ArrayList<>();
            LOGGER.debug("Adding GuestAccountEvent.GuestUpdated event handler.");
            events.add(new GuestAccountEvent.GuestUpdated(cmd.getEnrichedGuest()));
            
            // add a verify loyalty event for Kafka publishing 
            // if loyalty information is not null.
            if (cmd.getEnrichedGuest().getLoyaltyInformation() != null
                    || (cmd.getEnrichedGuest().getPersonalInformation() != null)) {
                LOGGER.debug("Adding GuestAccountEvent.VerifyLoyalty event handler.");
                events.add(new GuestAccountEvent.VerifyLoyalty(cmd.getEnrichedGuest()));
            }
            
            return ctx.thenPersistAll(events, () -> ctx.reply(Done.getInstance()));
        });
        
        builder.setEventHandler(GuestAccountEvent.GuestUpdated.class,
                evt -> {
                    LOGGER.debug("Executing GuestAccountEvent.GuestUpdated event handler.");
                    return new GuestAccountState(null, evt.getEnrichedGuest(), LocalDateTime.now().toString(),
                            GuestEventStatus.UPDATE);
                });
        
        builder.setEventHandler(GuestAccountEvent.VerifyLoyalty.class, evt -> {
            LOGGER.debug("Executing GuestAccountEvent.VerifyLoyalty event handler.");
            return new GuestAccountState(null, evt.getEnrichedGuest(), LocalDateTime.now().toString(),
                    GuestEventStatus.UPDATE);
        });
        
        return builder.build();
    }
}
