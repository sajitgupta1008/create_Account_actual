package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;
import static com.lightbend.lagom.javadsl.api.transport.Method.PUT;

public interface GuestAccountService extends Service {
    
    /**
     * Create a guest account from the given {@link Guest} and get back the user ID
     * representing the created resource.
     *
     * @return {@code String}
     */
    ServiceCall<Guest, String> createAccount();
    
    ServiceCall<Guest, JsonNode> updateAccount(String email);
    
    @Override
    default Descriptor descriptor() {
        return named("guestAccounts")
                .withCalls(
                        restCall(POST, "/v1/guestAccounts", this::createAccount),
                        restCall(POST, "/v1/guestAccounts/", this::createAccount),
                        restCall(PUT, "/v1/guestAccounts/:email", this::updateAccount)
                )
                .withAutoAcl(true);
    }
}
