package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.CONSTRAINT_VIOLATION;

public class InvalidEmailFormatException extends MiddlewareTransportException {
    
    public InvalidEmailFormatException() {
        super(TransportErrorCode.fromHttp(422), "The email is invalidly formatted.", CONSTRAINT_VIOLATION);
    }
}
