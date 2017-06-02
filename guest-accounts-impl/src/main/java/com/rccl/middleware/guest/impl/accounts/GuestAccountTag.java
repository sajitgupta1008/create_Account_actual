package com.rccl.middleware.guest.impl.accounts;

import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;

public class GuestAccountTag {
    
    private static final int SHARDS = 4;
    
    public static final AggregateEventShards<GuestAccountEvent> GUEST_ACCOUNT_EVENT_TAG =
            AggregateEventTag.sharded(GuestAccountEvent.class, "GuestAccountEvent", SHARDS);
}
