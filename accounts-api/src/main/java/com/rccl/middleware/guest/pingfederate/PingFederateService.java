package com.rccl.middleware.guest.pingfederate;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;
import static com.lightbend.lagom.javadsl.api.transport.Method.POST;

public interface PingFederateService extends Service {
    
    ServiceCall<PingFederateSubject, ReferenceId> generateReferenceId();
    
    @Override
    default Descriptor descriptor() {
        return named("pingfederate")
                .withCalls(
                        restCall(POST, "/", this::generateReferenceId)
                                .withResponseSerializer(new PingFederateMessageSerializer())
                )
                .withHeaderFilter(PingFederateHeaderFilter.INSTANCE)
                .withAutoAcl(true);
    }
}
