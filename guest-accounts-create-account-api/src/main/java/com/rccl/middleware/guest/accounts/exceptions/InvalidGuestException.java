package com.rccl.middleware.guest.accounts.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvalidGuestException extends MiddlewareTransportException {
    
    public static final InvalidGuestException INVALID_PASSWORD;
    
    static {
        Map<String, String> passwordError = new HashMap<>();
        passwordError.put("password", "The password must be between 8 and 32 characters, inclusive, "
                + "with at least one (1) letter and one (1) number.");
        INVALID_PASSWORD = new InvalidGuestException(passwordError);
    }
    
    private InvalidGuestExceptionMessage igem;
    
    public InvalidGuestException(Map<String, String> validationErrors) {
        this("The request body is improper.", validationErrors);
    }
    
    public InvalidGuestException(String errorMessage, Map<String, String> validationErrors) {
        super(TransportErrorCode.fromHttp(422), new InvalidGuestExceptionMessage(errorMessage));
        
        this.igem = (InvalidGuestExceptionMessage) super.exceptionMessage();
        igem.setValidationErrors(validationErrors);
    }
    
    @Override
    public InvalidGuestExceptionMessage exceptionMessage() {
        return this.igem;
    }
    
    public void setValidationErrors(Map<String, String> validationErrors) {
        this.igem.setValidationErrors(validationErrors);
    }
    
    public static final class InvalidGuestExceptionMessage extends MiddlewareExceptionMessage {
    
        private static final long serialVersionUID = 1L;
        
        private Map<String, String> validationErrors;
        
        public InvalidGuestExceptionMessage(String errorMessage) {
            super(errorMessage);
        }
        
        public Map<String, String> getValidationErrors() {
            return validationErrors;
        }
        
        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
        }
        
        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
        
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }
}
