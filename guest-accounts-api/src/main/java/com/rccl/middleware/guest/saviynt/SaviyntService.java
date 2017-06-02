package com.rccl.middleware.guest.saviynt;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.rccl.middleware.guest.saviynt.exceptions.SaviyntExceptionSerializer;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

public interface SaviyntService extends Service {
    
    ServiceCall<SaviyntGuest, JsonNode> getAllSecurityQuestions();
    
    ServiceCall<SaviyntGuest, JsonNode> getGuestQuestions(String propertytosearch, String email);
    
    ServiceCall<NotUsed, JsonNode> getGuestAccount(String propertytosearch, String email);
    
    ServiceCall<SaviyntSecurityQuestionAnswerValidation, JsonNode> getValidateSecurityQuestionAnswers();
    
    ServiceCall<SaviyntGuest, JsonNode> postGuestAccount();
    
    ServiceCall<SaviyntGuest, JsonNode> putGuestAccount();
    
    @Override
    default Descriptor descriptor() {
        return named("saviynt")
                .withCalls(
                        restCall(POST, "/ecm/ws/rest/v2/createUser", this::postGuestAccount),
                        restCall(PUT, "/ecm/ws/rest/v2/updateUser", this::putGuestAccount),
                        restCall(GET, "/ecm/ws/rest/v2/fetchUserQuestions?propertytosearch&email", this::getGuestQuestions),
                        restCall(GET, "/ecm/ws/rest/v2/getSecurityQuestions", this::getAllSecurityQuestions),
                        restCall(GET, "/ecm/ws/rest/v2/getUser?propertytosearch&email", this::getGuestAccount),
                        restCall(POST, "/ecm/ws/rest/v2/validateAnswers", this::getValidateSecurityQuestionAnswers)
                )
                .withExceptionSerializer(SaviyntExceptionSerializer.INSTANCE)
                .withHeaderFilter(SaviyntHeaderFilter.INSTANCE)
                .withAutoAcl(true);
    }
}
