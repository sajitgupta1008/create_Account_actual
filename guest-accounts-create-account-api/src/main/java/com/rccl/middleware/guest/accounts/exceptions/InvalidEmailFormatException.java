package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

public class InvalidEmailFormatException extends MiddlewareTransportException {
    
    public InvalidEmailFormatException() {
        super(TransportErrorCode.fromHttp(422), "The email is invalidly formatted.");
    }
}
