package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

public class ExistingGuestException extends MiddlewareTransportException {
    
    private static final String DEFAULT_MESSAGE = "An existing user was found with these details.";
    
    public ExistingGuestException() {
        super(TransportErrorCode.fromHttp(422), new MiddlewareExceptionMessage(DEFAULT_MESSAGE));
    }
}
