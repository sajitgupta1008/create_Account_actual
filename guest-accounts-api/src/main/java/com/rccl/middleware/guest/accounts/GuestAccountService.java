package com.rccl.middleware.guest.accounts;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.typesafe.config.ConfigFactory;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

public interface GuestAccountService extends Service {
    
    String GUEST_ACCOUNTS_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.guest-accounts.topic.name");
    
    String LINK_LOYALTY_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.link-loyalty.topic.name");
    
    HeaderServiceCall<Guest, JsonNode> createAccount();
    
    ServiceCall<Guest, NotUsed> updateAccount(String email);
    
    ServiceCall<JsonNode, NotUsed> updateAccountEnriched();
    
    ServiceCall<AccountCredentials, JsonNode> authenticateUser();
    
    ServiceCall<NotUsed, JsonNode> validateEmail(String email);
    
    ServiceCall<NotUsed, String> healthCheck();
    
    Topic<GuestEvent> guestAccountsTopic();
    
    Topic<Guest> linkLoyaltyTopic();
    
    @Override
    default Descriptor descriptor() {
        return named("guestAccounts")
                .withCalls(
                        restCall(POST, "/v1/guestAccounts", this::createAccount),
                        restCall(POST, "/v1/guestAccounts/", this::createAccount),
                        restCall(PUT, "/v1/guestAccounts/:email", this::updateAccount),
                        restCall(PUT, "/v1/guestAccounts/enriched", this::updateAccountEnriched),
                        restCall(POST, "/v1/guestAccounts/login", this::authenticateUser),
                        restCall(GET, "/v1/guestAccounts/:email/validation", this::validateEmail),
                        restCall(GET, "/v1/guestAccounts/health", this::healthCheck)
                )
                .withTopics(
                        topic(GUEST_ACCOUNTS_KAFKA_TOPIC, this::guestAccountsTopic),
                        topic(LINK_LOYALTY_KAFKA_TOPIC, this::linkLoyaltyTopic)
                )
                .withAutoAcl(true);
    }
}
