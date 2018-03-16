package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class LegacyLinkBookingCommand implements CompressedJsonable, PersistentEntity.ReplyType<Done> {
    
    private static final long serialVersionUID = 1L;
    
    final LegacyLinkBookingMessage legacyLinkBookingEvent;
}
