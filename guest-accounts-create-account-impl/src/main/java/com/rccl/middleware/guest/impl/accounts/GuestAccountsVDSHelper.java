package com.rccl.middleware.guest.impl.accounts;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.exceptions.ExistingVDSRecordException;
import com.rccl.middleware.vds.VDSService;
import com.rccl.middleware.vds.exceptions.VDSExceptionFactory;
import com.rccl.middleware.vds.requests.PatchVDSVirtualID;
import com.rccl.middleware.vds.requests.PatchVDSVirtualIDMod;
import com.rccl.middleware.vds.requests.PatchVDSVirtualIDParameters;
import com.rccl.middleware.vds.responses.GenericVDSResponse;
import com.rccl.middleware.vds.responses.WebShopperView;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.net.ConnectException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        final String vdsFilterString = StringUtils.replace("vdsid={},ou=shopper,dc=rccl,dc=com",
                "{}", webshopperId);
        return vdsService.patchVDSVirtualId(vdsFilterString)
                .invoke(this.createVDSRequestPayload(vdsId))
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
     * Generates {@link PatchVDSVirtualID} payload for VDS API addVDSVirtualID service call.
     *
     * @param vdsId the created account's VDS ID.
     * @return {@link PatchVDSVirtualID}
     */
    private PatchVDSVirtualID createVDSRequestPayload(String vdsId) {
        List<PatchVDSVirtualIDMod> mods = new ArrayList<>();
        mods.add(PatchVDSVirtualIDMod.builder().attribute("ismigrated").type("ADD")
                .values(Collections.singletonList("True"))
                .build());
        
        mods.add(PatchVDSVirtualIDMod.builder().attribute("virtualdirectoryservicesid").type("ADD")
                .values(Collections.singletonList(vdsId))
                .build());
        
        mods.add(PatchVDSVirtualIDMod.builder().attribute("creationuserid").type("ADD")
                .values(Collections.singletonList("Excalibur_User"))
                .build());
        
        mods.add(PatchVDSVirtualIDMod.builder().attribute("creationdtm").type("ADD")
                .values(Collections.singletonList(ZonedDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"))))
                .build());
        
        return PatchVDSVirtualID.builder()
                .parameters(PatchVDSVirtualIDParameters.builder().vdsVirtualIDMods(mods).build())
                .build();
    }
    
    /**
     * In order to mitigate the multiple matching WebShopper usernames, when
     * the migration scenario is encountered, we will mark ALL matching
     * WebShopper usernames as migrated through
     * {@link #invokeVDSAddVirtualIDService(String, String)}.
     * <p>
     * The list of migrated WebShoppers is returned to permit additional
     * analysis or consumption of any retrieved details.
     *
     * @param vdsId {@code String}
     * @param email {@code String}
     * @return {@code CompletionStage<List<WebShopperView>>}
     */
    protected CompletionStage<List<WebShopperView>> setAllMatchingWebShopperIdsAsMigrated(String vdsId, String email) {
        return vdsService.getWebShopperAttributes("uid=" + email)
                .invoke()
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause();
                    String throwableMessage = cause.getMessage();
                    
                    LOGGER.error("There was an error migrating all matching WebShopper IDs: " + throwable.getMessage());
                    
                    if (cause instanceof ConnectException || cause instanceof VDSExceptionFactory.GenericVDSException) {
                        throw new MiddlewareTransportException(
                                TransportErrorCode.ServiceUnavailable,
                                throwableMessage,
                                UNKNOWN_ERROR
                        );
                    }
                    
                    throw new MiddlewareTransportException(
                            TransportErrorCode.UnexpectedCondition,
                            throwableMessage,
                            UNKNOWN_ERROR
                    );
                })
                .thenApply(webShopperViewListWrapper -> {
                    if (webShopperViewListWrapper == null) {
                        return null;
                    }
                    
                    List<WebShopperView> views = webShopperViewListWrapper.getWebshopperViews();
                    
                    if (views == null) {
                        return Collections.emptyList();
                    }
                    
                    for (WebShopperView view : views) {
                        String webshopperUsername = view.getWebshopperUsername();
                        this.invokeVDSAddVirtualIDService(vdsId, webshopperUsername);
                    }
                    
                    return views;
                });
    }
}
