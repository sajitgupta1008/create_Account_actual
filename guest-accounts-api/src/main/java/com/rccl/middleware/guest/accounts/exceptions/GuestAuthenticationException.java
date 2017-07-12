package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

public class GuestAuthenticationException extends MiddlewareTransportException {
    
    private static final String DEFAULT_MESSAGE = "An existing user was found with these details.";
    
    public GuestAuthenticationException() {
        super(TransportErrorCode.fromHttp(422), new MiddlewareExceptionMessage(DEFAULT_MESSAGE));
    }
    
    public GuestAuthenticationException(int errorCode, String message) {
        super(TransportErrorCode.fromHttp(errorCode), new MiddlewareExceptionMessage(message));
    }
}
