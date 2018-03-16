package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import com.lightbend.lagom.serialization.CompressedJsonable;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class LegacyLinkBookingState implements CompressedJsonable {
    
    public static final LegacyLinkBookingState EMPTY_STATE = new LegacyLinkBookingState(null, null);
    
    private static final long serialVersionUID = 1L;
    
    private final LegacyLinkBookingMessage event;
    
    private final String timestamp;
}
