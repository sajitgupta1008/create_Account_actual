package com.rccl.middleware.guest.accounts.exceptions;

import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.VDS_RECORD_MIGRATED;

public class ExistingVDSRecordException extends MiddlewareTransportException {
    
    private static final String DEFAULT_MESSAGE = "The WebShopper ID provided may have been migrated already.";
    
    public ExistingVDSRecordException() {
        this(DEFAULT_MESSAGE);
    }
    
    public ExistingVDSRecordException(String message) {
        super(TransportErrorCode.InternalServerError, message, VDS_RECORD_MIGRATED);
    }
}
