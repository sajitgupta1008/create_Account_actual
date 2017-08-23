package com.rccl.middleware.guest.accounts;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.typesafe.config.ConfigFactory;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

public interface GuestAccountService extends Service {
    
    String LINK_LOYALTY_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.link-loyalty.topic.name");
    
    String VERIFY_LOYALTY_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.verify-loyalty.topic.name");
    
    ServiceCall<Guest, JsonNode> createAccount();
    
    ServiceCall<EnrichedGuest, JsonNode> updateAccountEnriched();
    
    ServiceCall<AccountCredentials, JsonNode> authenticateUser();
    
    ServiceCall<NotUsed, JsonNode> validateEmail(String email);
    
    ServiceCall<NotUsed, String> healthCheck();
    
    Topic<GuestEvent> linkLoyaltyTopic();
    
    Topic<EnrichedGuest> verifyLoyaltyTopic();
    
    @Override
    default Descriptor descriptor() {
        return named("guestAccounts")
                .withCalls(
                        restCall(POST, "/guestAccounts", this::createAccount),
                        restCall(POST, "/guestAccounts/", this::createAccount),
                        restCall(PUT, "/guestAccounts/enriched", this::updateAccountEnriched),
                        restCall(POST, "/guestAccounts/login", this::authenticateUser),
                        restCall(GET, "/guestAccounts/:email/validation", this::validateEmail),
                        restCall(GET, "/guestAccounts/health", this::healthCheck)
                )
                .withTopics(
                        topic(LINK_LOYALTY_KAFKA_TOPIC, this::linkLoyaltyTopic),
                        topic(VERIFY_LOYALTY_KAFKA_TOPIC, this::verifyLoyaltyTopic)
                )
                .withAutoAcl(true);
    }
}
