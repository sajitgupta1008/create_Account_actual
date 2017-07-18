package com.rccl.middleware.guest.impl.accounts;

import com.lightbend.lagom.serialization.CompressedJsonable;
import com.rccl.middleware.guest.accounts.Guest;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class GuestAccountState implements CompressedJsonable {
    
    private final Guest guest;
    
    private final String timestamp;
    
    private final GuestEventStatus event;
    
    public static GuestAccountState emptyState() {
        return new GuestAccountState(Guest.builder().build(), LocalDateTime.now().toString(), GuestEventStatus.CREATE);
    }
}
