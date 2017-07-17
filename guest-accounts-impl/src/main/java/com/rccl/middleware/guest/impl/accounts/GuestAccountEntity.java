package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuestAccountEntity extends PersistentEntity<GuestAccountCommand, GuestAccountEvent, GuestAccountState> {
    
    @Override
    @SuppressWarnings("unchecked")
    public Behavior initialBehavior(Optional<GuestAccountState> snapshotState) {
        BehaviorBuilder builder = newBehaviorBuilder(snapshotState.orElse(GuestAccountState.emptyState()));
        
        builder.setCommandHandler(GuestAccountCommand.CreateGuest.class, (cmd, ctx) -> {
            if (cmd.getGuest() == null) {
                ctx.invalidCommand("Guest cannot be null.");
                ctx.done();
            }
            
            List<GuestAccountEvent> events = new ArrayList<>();
            events.add(new GuestAccountEvent.GuestCreated(cmd.getGuest()));
            events.add(new GuestAccountEvent.LinkLoyalty(cmd.getGuest()));
            return ctx.thenPersistAll(events, () -> ctx.reply(Done.getInstance()));
        });
        
        builder.setEventHandler(GuestAccountEvent.GuestCreated.class,
                evt -> new GuestAccountState(evt.getGuest(),
                        LocalDateTime.now().toString(), GuestEventStatus.CREATE));
        
        builder.setEventHandler(GuestAccountEvent.LinkLoyalty.class, evt -> new GuestAccountState(evt.getGuest(),
                LocalDateTime.now().toString(), GuestEventStatus.CREATE));
        
        builder.setCommandHandler(GuestAccountCommand.UpdateGuest.class, (cmd, ctx) ->
                ctx.thenPersist(new GuestAccountEvent.GuestUpdated(cmd.getGuest()),
                        evt -> ctx.reply(Done.getInstance())));
        
        builder.setEventHandler(GuestAccountEvent.GuestUpdated.class,
                evt -> new GuestAccountState(evt.getGuest(), LocalDateTime.now().toString(), GuestEventStatus.UPDATE));
        
        return builder.build();
    }
}
