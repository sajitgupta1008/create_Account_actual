package com.rccl.middleware.guest.accounts;

import akka.NotUsed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.typesafe.config.ConfigFactory;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.Service.topic;
import static com.lightbend.lagom.javadsl.api.transport.Method.GET;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

public interface GuestAccountService extends Service {
    
    String KAFKA_TOPIC_NAME = ConfigFactory.load().getString("kafka.topic.name");
    
    /**
     * Create a guest account from the given {@link Guest} and get back the user ID
     * representing the created resource.
     *
     * @return {@code String}
     */
    ServiceCall<Guest, TextNode> createAccount();
    
    ServiceCall<Guest, JsonNode> updateAccount(String email);
    
    ServiceCall<NotUsed, JsonNode> validateEmail(String email);
    
    ServiceCall<NotUsed, JsonNode> getOptins(String email);
    
    ServiceCall<NotUsed, String> healthCheck();
    
    Topic<GuestEvent> guestAccountsTopic();
    
    @Override
    default Descriptor descriptor() {
        return named("guestAccounts")
                .withCalls(
                        restCall(POST, "/v1/guestAccounts", this::createAccount),
                        restCall(POST, "/v1/guestAccounts/", this::createAccount),
                        restCall(PUT, "/v1/guestAccounts/:email", this::updateAccount),
                        restCall(GET, "/v1/guestAccounts/:email/validation", this::validateEmail),
                        restCall(GET, "/v1/guestAccounts/:email/optins", this::getOptins),
                        restCall(GET, "/v1/guestAccounts/health", this::healthCheck)
                )
                .publishing(
                        topic(KAFKA_TOPIC_NAME, this::guestAccountsTopic)
                )
                .withAutoAcl(true);
    }
}
