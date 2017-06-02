package com.rccl.middleware.guest.pingfederate;

import com.lightbend.lagom.javadsl.api.ServiceCall;

import java.util.concurrent.CompletableFuture;

/**
 * This is a mock implementation of the {@link PingFederateService}.
 * <p>
 * It is intended to be leveraged the test classes for other services that depend on the PingFederate service internally.
 */
public class PingFederateServiceImplStub implements PingFederateService {
    
    public static final ReferenceId REFERENCE_ID =
            new ReferenceId("7436BAFA6FB89BE8E74EAE07E5ADD43E795ED726C0A75270533FAD9D3ACA");
    
    @Override
    public ServiceCall<PingFederateSubject, ReferenceId> generateReferenceId() {
        return subject -> CompletableFuture.completedFuture(REFERENCE_ID);
    }
}
