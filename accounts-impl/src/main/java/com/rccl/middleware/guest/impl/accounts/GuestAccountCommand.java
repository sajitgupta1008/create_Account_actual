package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.Guest;
import lombok.Builder;
import lombok.Value;

public interface GuestAccountCommand extends Jsonable {
    
    @Builder
    @Value
    final class CreateGuest implements GuestAccountCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
        final Guest guest;
    }
    
    @Builder
    @Value
    final class UpdateGuest implements GuestAccountCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
        final Guest guest;
    }
    
}
