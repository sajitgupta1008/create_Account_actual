package com.rccl.middleware.guest.saviynt.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightbend.lagom.javadsl.api.deser.ExceptionSerializer;
import com.lightbend.lagom.javadsl.api.deser.RawExceptionMessage;
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol;

import java.io.IOException;
import java.util.Collection;

public class SaviyntExceptionSerializer implements ExceptionSerializer {
    
    public static final SaviyntExceptionSerializer INSTANCE = new SaviyntExceptionSerializer();
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private SaviyntExceptionSerializer() {
        // No-op.
    }
    
    @Override
    public RawExceptionMessage serialize(Throwable throwable, Collection<MessageProtocol> collection) {
        return null;
    }
    
    @Override
    public Throwable deserialize(RawExceptionMessage rem) {
        ErrorPayload ep = this.mapRawExceptionMessageToErrorPayload(rem);
        
        return SaviyntExceptionFactory.getInstance(ep);
    }
    
    private ErrorPayload mapRawExceptionMessageToErrorPayload(RawExceptionMessage rem) {
        ErrorPayload ep;
        
        try {
            String errorJson = rem.messageAsText();
            ep = OBJECT_MAPPER.readValue(errorJson, ErrorPayload.class);
            
            if (ep.getErrorMessage() == null) {
                ep.setErrorMessage("No error message was provided.");
            }
            
            if (ep.getErrorCode() == null) {
                ep.setErrorCode(-1);
            }
            
            if (ep.getSaviyntErrorCode() == null) {
                ep.setSaviyntErrorCode("Unknown");
            }
            
            ep.setStatusCode(rem.errorCode().http());
        } catch (IOException ioe) {
            ep = new ErrorPayload();
            ep.setErrorCode(-1);
            ep.setStatusCode(-1);
            ep.setErrorMessage("No payload available.");
            ep.setSaviyntErrorCode("Unknown");
        }
        
        return ep;
    }
}
