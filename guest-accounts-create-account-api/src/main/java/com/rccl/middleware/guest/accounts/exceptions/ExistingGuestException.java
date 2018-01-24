package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.EXISTING_GUEST;

public class ExistingGuestException extends MiddlewareTransportException {
    
    private static final String DEFAULT_MESSAGE = "An existing user was found with these details.";
    
    public ExistingGuestException() {
        super(TransportErrorCode.fromHttp(422), DEFAULT_MESSAGE, EXISTING_GUEST);
    }
}
