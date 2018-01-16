package com.rccl.middleware.guest.impl.accounts;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.exceptions.ExistingVDSRecordException;
import com.rccl.middleware.vds.VDSService;
import com.rccl.middleware.vds.exceptions.VDSExceptionFactory;
import com.rccl.middleware.vds.requests.VDSVirtualID;
import com.rccl.middleware.vds.requests.VDSVirtualIDAttributes;
import com.rccl.middleware.vds.requests.VDSVirtualIDParameters;
import com.rccl.middleware.vds.responses.GenericVDSResponse;

import javax.inject.Inject;
import java.net.ConnectException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;

import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.UNKNOWN_ERROR;

/**
 * Helper class for any VDS API related service calls.
 */
public class GuestAccountsVDSHelper {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(GuestAccountsVDSHelper.class);
    
    private final VDSService vdsService;
    
    @Inject
    public GuestAccountsVDSHelper(VDSService vdsService) {
        this.vdsService = vdsService;
    }
    
    /**
     * Invokes VDS API addVDSVirtualId service to flag a WebShopper account as migrated in VDS by specifying
     * {@code isMigrated} to "True".
     *
     * @param webshopperId the account WebShopper from the service request payload.
     * @param vdsId        the created account's VDS ID.
     * @return {@link CompletionStage}<{@link GenericVDSResponse}>
     */
    protected CompletionStage<GenericVDSResponse> invokeVDSAddVirtualIDService(String webshopperId, String vdsId) {
        LOGGER.debug("Invoking VDS Virtual ID to flag isMigrated for VDS ID {} and WebShopper ID {} to true.",
                vdsId, webshopperId);
        
        return vdsService.addVDSVirtualId()
                .invoke(this.createVDSRequestPayload(webshopperId, vdsId))
                .exceptionally(throwable -> {
                    LOGGER.error("An error occurred when invoking VDS Virtual ID service.", throwable);
                    Throwable cause = throwable.getCause();
                    if (cause instanceof ConnectException) {
                        throw new MiddlewareTransportException(TransportErrorCode.ServiceUnavailable,
                                throwable.getMessage(), UNKNOWN_ERROR);
                    } else if (cause instanceof VDSExceptionFactory.ExistingVirtualIdentityException) {
                        throw new ExistingVDSRecordException();
                    }
                    throw new MiddlewareTransportException(TransportErrorCode.InternalServerError,
                            throwable.getMessage(), UNKNOWN_ERROR);
                })
                .thenApply(response -> response);
    }
    
    /**
     * Generates {@link VDSVirtualID} payload for VDS API addVDSVirtualID service call.
     *
     * @param webshopperId the account WebShopper from the service request payload.
     * @param vdsId        the created account's VDS ID.
     * @return {@link VDSVirtualID}
     */
    private VDSVirtualID createVDSRequestPayload(String webshopperId, String vdsId) {
        return VDSVirtualID.builder()
                .parameters(VDSVirtualIDParameters.builder()
                        .ldapSearch(String.format("vdsid=%s,ou=shopper_virtualid,o=webshopper", webshopperId))
                        .attributes(VDSVirtualIDAttributes.builder()
                                .isMigrated("True")
                                .shopperIdMain(webshopperId)
                                .shopperIdMinor(webshopperId)
                                .vdsId(vdsId)
                                .creationTimestamp(ZonedDateTime.now(ZoneOffset.UTC)
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")))
                                .build())
                        .build())
                .build();
    }
}
