package com.rccl.middleware.guest.impl.saviynt;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.rccl.middleware.guest.saviynt.SaviyntGuest;
import com.rccl.middleware.guest.saviynt.SaviyntSecurityQuestionAnswerValidation;
import com.rccl.middleware.guest.saviynt.SaviyntService;
import com.rccl.middleware.guest.saviynt.exceptions.SaviyntExceptionFactory;

import java.util.concurrent.CompletableFuture;

/**
 * This is a mock implementation of the {@link com.rccl.middleware.guest.saviynt.SaviyntService}.
 * <p>
 * It is intended to be leveraged the test classes for other services that depend on the Saviynt service internally.
 */
public class SaviyntServiceImplStub implements SaviyntService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public ServiceCall<SaviyntGuest, JsonNode> getAllSecurityQuestions() {
        return null;
    }
    
    @Override
    public ServiceCall<SaviyntGuest, JsonNode> getGuestQuestions(String propertytosearch, String email) {
        return null;
    }
    
    @Override
    public ServiceCall<NotUsed, JsonNode> getGuestAccount(String propertytosearch, String email) {
        return null;
    }
    
    @Override
    public ServiceCall<SaviyntSecurityQuestionAnswerValidation, JsonNode> getValidateSecurityQuestionAnswers() {
        return null;
    }
    
    @Override
    public ServiceCall<SaviyntGuest, JsonNode> postGuestAccount() {
        return saviyntGuest -> {
            // If the account already exists...
            if ("existing@email.com".equalsIgnoreCase(saviyntGuest.getEmail())) {
                throw new SaviyntExceptionFactory.ExistingGuestException(null);
            }
            
            ObjectNode response = OBJECT_MAPPER.createObjectNode();
            
            response.put("message", "The user " + saviyntGuest.getEmail() + " was successfully created.");
            response.put("errorCode", 0);
            
            return CompletableFuture.completedFuture(response);
        };
    }
    
    @Override
    public ServiceCall<SaviyntGuest, JsonNode> putGuestAccount() {
        return (request) -> {
            // If the account exists, successful update...
            if ("successful@domain.com".equals(request.getEmail())) {
                ObjectNode responseJson = OBJECT_MAPPER.createObjectNode();
                
                responseJson.put("message", "Test Account with accountId uid=successful@domain.com,ou=guest,dc=rccl,dc=com Updated Successfully");
                responseJson.put("errorCode", "0");
                responseJson.put("SavCode", "Sav000");
                
                return CompletableFuture.completedFuture(responseJson);
            }
            
            throw new SaviyntExceptionFactory.NoSuchGuestException(null);
        };
    }
}
