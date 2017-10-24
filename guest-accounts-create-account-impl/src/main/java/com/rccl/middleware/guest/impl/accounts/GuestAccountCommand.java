package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import lombok.Builder;
import lombok.Value;

public interface GuestAccountCommand extends Jsonable {
    
    @Builder
    @Value
    final class CreateGuest implements GuestAccountCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
    
        private static final long serialVersionUID = 1L;
        
        final Guest guest;
    }
    
    @Builder
    @Value
    final class UpdateGuest implements GuestAccountCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
    
        private static final long serialVersionUID = 1L;
        
        final EnrichedGuest enrichedGuest;
    }
    
}
