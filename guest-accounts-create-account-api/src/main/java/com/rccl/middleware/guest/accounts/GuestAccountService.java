package com.rccl.middleware.guest.accounts;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.CircuitBreaker;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.rccl.middleware.akka.clustermanager.models.ActorSystemInformation;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import com.typesafe.config.ConfigFactory;

import java.util.Optional;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

// Because Lagom only supports optional requests parameters as Optionals
// we can safely suppress this warning.
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface GuestAccountService extends Service {
    
    String LEGACY_LINK_BOOKING_TOPIC = ConfigFactory.load().getString("kafka.legacy-link-booking.topic.name");
    
    String LINK_LOYALTY_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.link-loyalty.topic.name");
    
    String VERIFY_LOYALTY_KAFKA_TOPIC = ConfigFactory.load().getString("kafka.verify-loyalty.topic.name");
    
    ServiceCall<Guest, ResponseBody<JsonNode>> createAccount();
    
    ServiceCall<NotUsed, ResponseBody<EnrichedGuest>> getAccountEnriched(String vdsId, Optional<String> extended);
    
    ServiceCall<EnrichedGuest, ResponseBody<JsonNode>> updateAccountEnriched();
    
    ServiceCall<NotUsed, ResponseBody<JsonNode>> validateEmail(String email, Optional<String> inputType);
    
    ServiceCall<NotUsed, ResponseBody<ActorSystemInformation>> akkaClusterHealthCheck();
    
    Topic<GuestEvent> linkLoyaltyTopic();
    
    Topic<EnrichedGuest> verifyLoyaltyTopic();
    
    Topic<LegacyLinkBookingMessage> legacyLinkBookingTopic();
    
    @Override
    default Descriptor descriptor() {
        return named("guest_accounts_create_account")
                .withCalls(
                        restCall(POST, "/guestAccounts", this::createAccount),
                        restCall(POST, "/guestAccounts/", this::createAccount),
                        restCall(GET, "/guestAccounts/enriched/:vdsId?extended", this::getAccountEnriched),
                        restCall(PUT, "/guestAccounts/enriched", this::updateAccountEnriched),
                        restCall(GET, "/guestAccounts/:email/validation?inputType", this::validateEmail),
                        restCall(GET, "/akkaCluster/health", this::akkaClusterHealthCheck)
                )
                .withTopics(
                        topic(LEGACY_LINK_BOOKING_TOPIC, this::legacyLinkBookingTopic),
                        topic(LINK_LOYALTY_KAFKA_TOPIC, this::linkLoyaltyTopic),
                        topic(VERIFY_LOYALTY_KAFKA_TOPIC, this::verifyLoyaltyTopic)
                )
                .withCircuitBreaker(CircuitBreaker.identifiedBy("guest_accounts_create_account"))
                .withAutoAcl(true);
    }
}
