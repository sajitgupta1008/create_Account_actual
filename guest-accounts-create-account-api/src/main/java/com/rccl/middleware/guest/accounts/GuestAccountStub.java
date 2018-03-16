package com.rccl.middleware.guest.accounts;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.rccl.middleware.akka.clustermanager.models.ActorSystemInformation;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.guest.accounts.email.EmailNotification;
import com.rccl.middleware.guest.accounts.enriched.ContactInformation;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.enriched.SignInInformation;
import com.rccl.middleware.guest.accounts.enriched.TravelDocumentInformation;
import com.rccl.middleware.guest.accounts.enriched.WebshopperInformation;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import com.rccl.middleware.guestprofiles.models.Address;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GuestAccountStub implements GuestAccountService {
    
    @Override
    public ServiceCall<NotUsed, ResponseBody<EnrichedGuest>> getAccountEnriched(String vdsId,
                                                                                Optional<String> extended) {
        return response -> {
            EnrichedGuest enrichedGuest = EnrichedGuest.builder()
                    .header(Header.builder().brand('R').channel("app-ios").build())
                    .contactInformation(ContactInformation.builder()
                            .address(Address.builder()
                                    .addressOne("Address one")
                                    .city("City")
                                    .state("FL")
                                    .zipCode("12345")
                                    .build())
                            .phoneNumber("123-456-7890")
                            .phoneCountryCode("+1")
                            .build())
                    .emergencyContact(EmergencyContact.builder()
                            .phoneNumber("123-456-7890")
                            .firstName("First")
                            .lastName("Last")
                            .relationship("Mother")
                            .build())
                    .signInInformation(SignInInformation.builder()
                            .password("password1".toCharArray())
                            .securityQuestions(
                                    Collections.singletonList(SecurityQuestion.builder()
                                            .question("what?").answer("yes").build())
                            )
                            .build())
                    .travelDocumentInformation(TravelDocumentInformation.builder()
                            .passportNumber("1234567890")
                            .passportExpirationDate("20200101")
                            .birthCountryCode("USA")
                            .citizenshipCountryCode("USA")
                            .build())
                    .webshopperInformation(WebshopperInformation.builder()
                            .brand('R').shopperId("123456789").build())
                    .consumerId("1234567")
                    .email("successful@domain.com")
                    .vdsId("G1234567")
                    .build();
            
            return CompletableFuture.completedFuture(ResponseBody.<EnrichedGuest>builder()
                    .status(ResponseHeader.OK.status())
                    .payload(enrichedGuest).build());
        };
    }
    
    @Override
    public ServiceCall<EnrichedGuest, ResponseBody<JsonNode>> updateAccountEnriched() {
        return request -> CompletableFuture.completedFuture(ResponseBody.<JsonNode>builder().build());
    }
    
    @Override
    public ServiceCall<NotUsed, ResponseBody<JsonNode>> validateEmail(String email, Optional<String> inputType) {
        return request -> CompletableFuture.completedFuture(ResponseBody.<JsonNode>builder().build());
    }
    
    @Override
    public ServiceCall<Guest, ResponseBody<JsonNode>> createAccount() {
        return request -> CompletableFuture.completedFuture(ResponseBody.<JsonNode>builder().build());
    }
    
    @Override
    public Topic<EmailNotification> emailNotificationTopic() {
        return null;
    }
    
    @Override
    public Topic<EnrichedGuest> verifyLoyaltyTopic() {
        return null;
    }
    
    @Override
    public Topic<GuestEvent> linkLoyaltyTopic() {
        return null;
    }
    
    @Override
    public Topic<LegacyLinkBookingMessage> legacyLinkBookingTopic() {
        return null;
    }
    
    @Override
    public ServiceCall<NotUsed, ResponseBody<ActorSystemInformation>> akkaClusterHealthCheck() {
        return request -> CompletableFuture.completedFuture(ResponseBody.<ActorSystemInformation>builder().build());
    }
}
