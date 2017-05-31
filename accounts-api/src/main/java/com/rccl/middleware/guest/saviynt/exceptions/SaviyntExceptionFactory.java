package com.rccl.middleware.guest.saviynt.exceptions;

import lombok.Getter;

public class SaviyntExceptionFactory {
    
    public static SaviyntException getInstance(ErrorPayload ep) {
        if (ep == null) {
            return null;
        }
        
        return determineExceptionType(ep);
    }
    
    public static SaviyntException determineExceptionType(ErrorPayload ep) {
        String saviyntErrorCode = ep.getSaviyntErrorCode();
        
        switch (saviyntErrorCode) {
            case "Sav005": // Error from LDAP.
            case "Sav009": // Guest already exists.
                return new ExistingGuestException(ep);
            
            case "Sav006": // Both question and answer are required.
            case "Sav0014": // Answer is missing from request.
            case "Sav0019": // Question is blank.
            case "Sav0020": // Answer is blank.
                return new InvalidSecurityQuestionOrAnswerException(ep);
            
            case "Sav007": // Password validation failed.
                return new InvalidPasswordFormatException(ep);
            
            case "Sav0010": // Email validation failed.
                return new InvalidEmailFormatException(ep);
            
            case "Sav0011": // "Property to search" field is missing.
                return new MissingPropertyToSearchFieldException(ep);
            
            case "Sav0012": // Multiple guest found.
            case "Sav0017": // Multiple accounts found.
                return new AmbiguousDetailsException(ep);
            
            case "Sav0015": // No such guest.
            case "Sav0016": // No such account.
                return new NoSuchGuestException(ep);
            
            case "Sav0018": // Username cannot be blank.
                return new MissingUsernameException(ep);
            
            
            case "Sav0021": // Username cannot be updated.
                return new IllegalUpdateException(ep);
            
            case "Sav001": // LDAP not set to true.
            case "Sav002": // Endpoint not specified.
            case "Sav003": // Error while creating record.
            case "Sav004": // Error while updating record.
            case "Sav008": // Invalid status key.
            case "Sav0013": // Unexpected error occurred.
            default:
                return new SaviyntEnvironmentException(ep);
        }
    }
    
    public static class InvalidSecurityQuestionOrAnswerException extends SaviyntException {
        
        public InvalidSecurityQuestionOrAnswerException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class IllegalUpdateException extends SaviyntException {
        
        public IllegalUpdateException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class AmbiguousDetailsException extends SaviyntException {
        
        public AmbiguousDetailsException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class MissingUsernameException extends SaviyntException {
        
        public MissingUsernameException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class MissingPropertyToSearchFieldException extends SaviyntException {
        
        public MissingPropertyToSearchFieldException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class InvalidEmailFormatException extends SaviyntException {
        
        public InvalidEmailFormatException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class InvalidPasswordFormatException extends SaviyntException {
        
        public InvalidPasswordFormatException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class NoSuchGuestException extends SaviyntException {
        
        public NoSuchGuestException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class ExistingGuestException extends SaviyntException {
        
        public ExistingGuestException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    public static class SaviyntEnvironmentException extends SaviyntException {
        
        public SaviyntEnvironmentException(ErrorPayload ep) {
            super(ep);
        }
    }
    
    @Getter
    public static class SaviyntException extends RuntimeException {
        
        private Integer errorCode;
        
        private String errorMessage;
        
        private String saviyntErrorCode;
        
        private Integer statusCode;
        
        public SaviyntException(ErrorPayload ep) {
            if (ep != null) {
                this.errorCode = ep.getErrorCode();
                this.errorMessage = ep.getErrorMessage();
                this.saviyntErrorCode = ep.getSaviyntErrorCode();
                this.statusCode = ep.getStatusCode();
            }
        }
        
        public boolean isExistingGuestException() {
            return this.saviyntErrorCode == "SAV0004" || this.saviyntErrorCode == "SAV0005";
        }
    }
}
