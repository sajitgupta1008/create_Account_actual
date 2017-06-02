package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

public class GuestNotFoundException extends MiddlewareTransportException {
    
    private static final String DEFAULT_MESSAGE = "User with details provided was not found.";
    
    public GuestNotFoundException() {
        super(TransportErrorCode.fromHttp(422), DEFAULT_MESSAGE);
    }
}
