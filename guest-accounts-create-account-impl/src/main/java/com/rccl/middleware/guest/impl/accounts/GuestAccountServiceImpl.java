package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.japi.Pair;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.rccl.middleware.common.exceptions.MiddlewareError;
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.common.request.EnvironmentDetails;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.common.validation.MiddlewareValidation;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.guest.accounts.AccountStatusEnum;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.email.EmailNotification;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.exceptions.ExistingGuestException;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidEmailFormatException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidPasswordException;
import com.rccl.middleware.guest.authentication.AccountCredentials;
import com.rccl.middleware.guest.authentication.GuestAuthenticationService;
import com.rccl.middleware.guest.impl.accounts.email.AccountCreatedConfirmationEmail;
import com.rccl.middleware.guest.impl.accounts.email.EmailNotificationEntity;
import com.rccl.middleware.guest.impl.accounts.email.EmailNotificationTag;
import com.rccl.middleware.guest.impl.accounts.email.EmailUpdatedConfirmationEmail;
import com.rccl.middleware.guest.impl.accounts.email.PasswordUpdatedConfirmationEmail;
import com.rccl.middleware.guest.optin.EmailOptins;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.PostalOptins;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.guestprofiles.models.Profile;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import com.rccl.middleware.saviynt.api.requests.SaviyntAuthAccountPassword;
import com.rccl.middleware.saviynt.api.requests.SaviyntGuest;
import com.rccl.middleware.saviynt.api.responses.AccountInformation;
import com.rccl.middleware.saviynt.api.responses.AccountStatus;
import com.rccl.middleware.saviynt.api.responses.GenericSaviyntResponse;
import com.rccl.middleware.vds.responses.GenericVDSResponse;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.CONSTRAINT_VIOLATION;
import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.MULTIPLE_BACKEND_ERROR;
import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.SIGN_IN_ERROR;
import static com.rccl.middleware.guest.accounts.exceptions.CreateAccountErrorCodeContants.UNKNOWN_ERROR;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(GuestAccountServiceImpl.class);
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String APPKEY_HEADER = "AppKey";
    
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    
    private static final String DEFAULT_ACCEPT_LANGUAGE_HEADER = "en";
    
    private static final String DEFAULT_APP_KEY = ConfigFactory.load().getString("default.apigee.appkey");
    
    private final AccountCreatedConfirmationEmail accountCreatedConfirmationEmail;
    
    private final EmailUpdatedConfirmationEmail emailUpdatedConfirmationEmail;
    
    private final PasswordUpdatedConfirmationEmail passwordUpdatedConfirmationEmail;
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final SaviyntService saviyntService;
    
    private final GuestProfilesService guestProfilesService;
    
    private final GuestProfileOptinService guestProfileOptinService;
    
    private final GuestAuthenticationService guestAuthenticationService;
    
    private final GuestAccountsVDSHelper vdsHelper;
    
    @Inject
    public GuestAccountServiceImpl(AccountCreatedConfirmationEmail accountCreatedConfirmationEmail,
                                   EmailUpdatedConfirmationEmail emailUpdatedConfirmationEmail,
                                   PasswordUpdatedConfirmationEmail passwordUpdatedConfirmationEmail,
                                   SaviyntService saviyntService,
                                   PersistentEntityRegistry persistentEntityRegistry,
                                   GuestProfilesService guestProfilesService,
                                   GuestProfileOptinService guestProfileOptinService,
                                   GuestAuthenticationService guestAuthenticationService,
                                   GuestAccountsVDSHelper vdsHelper) {
        
        this.saviyntService = saviyntService;
        this.vdsHelper = vdsHelper;
        
        this.guestProfilesService = guestProfilesService;
        this.guestProfileOptinService = guestProfileOptinService;
        this.guestAuthenticationService = guestAuthenticationService;
        
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(GuestAccountEntity.class);
        persistentEntityRegistry.register(EmailNotificationEntity.class);
        
        this.accountCreatedConfirmationEmail = accountCreatedConfirmationEmail;
        this.emailUpdatedConfirmationEmail = emailUpdatedConfirmationEmail;
        this.passwordUpdatedConfirmationEmail = passwordUpdatedConfirmationEmail;
    }
    
    @Override
    public HeaderServiceCall<Guest, ResponseBody<JsonNode>> createAccount() {
        return (requestHeader, guest) -> {
            MiddlewareValidation.validateWithGroups(guest, CONSTRAINT_VIOLATION, Guest.CreateChecks.class);
            
            final SaviyntGuest saviyntGuest = Mapper.mapGuestToSaviyntGuest(guest, true).build();
            
            String languageCode = requestHeader.getHeader(ACCEPT_LANGUAGE_HEADER).orElse(DEFAULT_ACCEPT_LANGUAGE_HEADER);
            
            return saviyntService
                    .createGuestAccount()
                    .invoke(saviyntGuest)
                    .exceptionally(throwable -> {
                        LOGGER.error("Saviynt Create Account failed. ", throwable);
                        
                        Throwable cause = throwable.getCause();
                        if (cause instanceof ConnectException
                                || cause instanceof SaviyntExceptionFactory.SaviyntEnvironmentException) {
                            throw new MiddlewareTransportException(TransportErrorCode.ServiceUnavailable,
                                    throwable.getMessage(), UNKNOWN_ERROR);
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.ExistingGuestException) {
                            throw new ExistingGuestException();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidEmailFormatException();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidPasswordFormatException) {
                            throw new InvalidPasswordException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500),
                                throwable.getMessage(), UNKNOWN_ERROR);
                    })
                    .thenCompose(response -> {
                        String message = response.getMessage();
                        Pattern pattern = Pattern.compile("vdsid=[a-zA-Z0-9]*");
                        Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        
                        String vdsId = matcher.group(0).substring(6);
                        
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(Mapper.mapVdsIdWithGuest(vdsId, guest)));
                        
                        // if WebShopper ID is present, invoke VDS API to immediately flag isMigrated to true
                        // instead of waiting for async IAM process.
                        CompletionStage<GenericVDSResponse> vdsService;
                        if (StringUtils.isNoneBlank(saviyntGuest.getWebshopperId())) {
                            vdsService = vdsHelper.invokeVDSAddVirtualIDService(saviyntGuest.getWebshopperId(), vdsId);
                        } else {
                            vdsService = CompletableFuture.completedFuture(null);
                        }
                        
                        return vdsService.thenCompose(genericVDSResponse -> {
                            if ("web".equals(guest.getHeader().getChannel())) {
                                ObjectNode objNode = OBJECT_MAPPER.createObjectNode();
                                objNode.put("vdsId", vdsId);
                                
                                // Send the account created confirmation email.
                                accountCreatedConfirmationEmail.send(guest, languageCode);
                                
                                return CompletableFuture.completedFuture(
                                        Pair.create(ResponseHeader.OK.withStatus(201), ResponseBody
                                                .<JsonNode>builder()
                                                .status(201)
                                                .payload(objNode)
                                                .build()));
                            } else {
                                String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
                                // automatically authenticate user and include vdsId in the response.
                                return guestAuthenticationService.authenticateUser()
                                        .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                                        .invoke(AccountCredentials.builder()
                                                .header(guest.getHeader())
                                                .username(guest.getEmail())
                                                .password(guest.getPassword())
                                                .build())
                                        .exceptionally(throwable -> {
                                            LOGGER.error("User Authentication failed for email {}.",
                                                    guest.getEmail(), throwable);
                                            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(401),
                                                    throwable.getMessage(), SIGN_IN_ERROR);
                                        })
                                        .thenApply(authResponse -> {
                                            // Send the account created confirmation email.
                                            accountCreatedConfirmationEmail.send(guest, languageCode);
                                            
                                            return Pair.create(ResponseHeader.OK.withStatus(201), authResponse);
                                        });
                            }
                        });
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, ResponseBody<EnrichedGuest>> getAccountEnriched(String vdsId,
                                                                                      Optional<String> extended) {
        return (requestHeader, notUsed) -> {
            
            if (StringUtils.isBlank(vdsId)) {
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422),
                        new MiddlewareExceptionMessage(CONSTRAINT_VIOLATION, null, "VDS ID is required."));
            }
            
            String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
            
            // In case of exception, return null and let the other process go through to return whichever
            // attributes are available.
            final CompletionStage<ResponseBody<Profile>> getProfile = guestProfilesService.getProfile(vdsId)
                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                    .invoke().exceptionally(throwable -> {
                        LOGGER.error("GET Profile failed for VDS ID {}.", vdsId, throwable);
                        return null;
                    });
            
            return this.getAccount(vdsId).invoke()
                    .exceptionally(throwable -> {
                        LOGGER.error("GET Guest Account failed for VDS ID {}.", vdsId, throwable);
                        return null;
                    })
                    .thenCombineAsync(getProfile, (guest, profile) -> {
                        if (guest == null && profile == null) {
                            throw new GuestNotFoundException();
                        }
                        return Pair.create(guest, profile);
                    }).thenCompose(pair -> {
                        Guest guest = pair.first();
                        ResponseBody<Profile> profile = pair.second();
                        
                        String extendedView = extended.orElse("false");
                        
                        return this.getOptins(guest, appKey).thenApply(optinsPair -> {
                            EnrichedGuest enrichedGuest = this.filterObjectView(
                                    Mapper.mapToEnrichedGuest(guest, profile, optinsPair.first(), optinsPair.second()),
                                    extendedView.equalsIgnoreCase("true")
                                            ? EnrichedGuest.ExtendedView.class : EnrichedGuest.DefaultView.class);
                            return Pair.create(ResponseHeader.OK, ResponseBody
                                    .<EnrichedGuest>builder()
                                    .status(ResponseHeader.OK.status())
                                    .payload(enrichedGuest)
                                    .build());
                        });
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<EnrichedGuest, ResponseBody<JsonNode>> updateAccountEnriched() {
        return (requestHeader, enrichedGuest) -> {
            CompletionStage<AccountInformation> originalSaviyntAccount = saviyntService
                    .getGuestAccount("systemUserName",
                            Optional.empty(),
                            Optional.of(enrichedGuest.getVdsId()))
                    .invoke();
            
            MiddlewareValidation.validate(enrichedGuest, CONSTRAINT_VIOLATION);
            MiddlewareValidation.validateWithGroups(enrichedGuest.getEmailOptins(),
                    CONSTRAINT_VIOLATION, EmailOptins.DefaultChecks.class);
            MiddlewareValidation.validateWithGroups(enrichedGuest.getPostalOptins(),
                    CONSTRAINT_VIOLATION, PostalOptins.DefaultChecks.class);
            
            String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
            String languageCode = requestHeader.getHeader(ACCEPT_LANGUAGE_HEADER).orElse(DEFAULT_ACCEPT_LANGUAGE_HEADER);
            
            CompletionStage<NotUsed> updateAccountService = CompletableFuture.completedFuture(NotUsed.getInstance());
            Guest.GuestBuilder guestBuilder = Mapper.mapEnrichedGuestToGuest(enrichedGuest);
            
            if (!guestBuilder.build().equals(Guest.builder().build())) {
                final Guest guest = guestBuilder
                        .header(enrichedGuest.getHeader())
                        .email(enrichedGuest.getEmail())
                        .vdsId(enrichedGuest.getVdsId())
                        .build();
                updateAccountService = this.updateAccount()
                        .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey)).invoke(guest);
            }
            
            CompletionStage<ResponseBody<TextNode>> updateProfileService =
                    CompletableFuture.completedFuture(ResponseBody
                            .<TextNode>builder().payload(TextNode.valueOf(enrichedGuest.getVdsId())).build());
            Profile.ProfileBuilder profileBuilder = Mapper.mapEnrichedGuestToProfile(enrichedGuest);
            
            EnvironmentDetails environmentDetails;
            
            try {
                environmentDetails = EnvironmentDetails.getInstance(requestHeader);
            } catch (IllegalArgumentException iae) {
                LOGGER.error("The Environment-Marker and Environment-Ship-Code headers are missing.", iae);
                MiddlewareError me = MiddlewareError.builder()
                        .developerMessage("The Environment-Marker and Environment-Ship-Code headers are missing."
                                + " Please verify Apigee is passing them in.")
                        .userMessage("The Environment-Marker and Environment-Ship-Code headers "
                                + "are missing on this request.")
                        .errorCode(UNKNOWN_ERROR)
                        .build();
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422), me);
            }
            
            if (!profileBuilder.build().equals(Profile.builder().build())) {
                final Profile profile = profileBuilder.vdsId(enrichedGuest.getVdsId()).build();
                
                updateProfileService = guestProfilesService.updateProfile()
                        .handleRequestHeader(rh -> rh
                                .withHeader(APPKEY_HEADER, appKey)
                                .withHeader(EnvironmentDetails.ENVIRONMENT_MARKER_HEADER_NAME,
                                        environmentDetails.getEnvironmentMarker())
                                .withHeader(EnvironmentDetails.ENVIRONMENT_SHIP_CODE_HEADER_NAME,
                                        environmentDetails.getEnvironmentShipCode())
                        ).invoke(profile);
            }
            
            CompletionStage<ResponseBody> updateEmailOptinsService = CompletableFuture
                    .completedFuture(ResponseBody.builder().build());
            EmailOptins emailOptins = Mapper.mapEnrichedGuestToEmailOptins(enrichedGuest);
            
            if (emailOptins != null && StringUtils.isNotBlank(enrichedGuest.getEmail())) {
                updateEmailOptinsService = guestProfileOptinService
                        .updateEmailOptins(enrichedGuest.getEmail())
                        .handleRequestHeader(rh -> rh
                                .withHeader(APPKEY_HEADER, appKey)
                                .withHeader(EnvironmentDetails.ENVIRONMENT_MARKER_HEADER_NAME,
                                        environmentDetails.getEnvironmentMarker())
                                .withHeader(EnvironmentDetails.ENVIRONMENT_SHIP_CODE_HEADER_NAME,
                                        environmentDetails.getEnvironmentShipCode())
                        ).invoke(emailOptins);
            }
            
            CompletionStage<ResponseBody> updatePostalOptinsService = CompletableFuture
                    .completedFuture(ResponseBody.builder().build());
            PostalOptins postalOptins = Mapper.mapEnrichedGuestToPostalOptins(enrichedGuest);
            
            if (postalOptins != null && StringUtils.isNotBlank(enrichedGuest.getEmail())) {
                updatePostalOptinsService = guestProfileOptinService
                        .updatePostalOptins(enrichedGuest.getVdsId())
                        .handleRequestHeader(rh -> rh
                                .withHeader(APPKEY_HEADER, appKey)
                                .withHeader(EnvironmentDetails.ENVIRONMENT_MARKER_HEADER_NAME,
                                        environmentDetails.getEnvironmentMarker())
                                .withHeader(EnvironmentDetails.ENVIRONMENT_SHIP_CODE_HEADER_NAME,
                                        environmentDetails.getEnvironmentShipCode())
                        ).invoke(postalOptins);
            }
            
            final CompletableFuture<NotUsed> accountFuture = updateAccountService.toCompletableFuture();
            final CompletableFuture<ResponseBody<TextNode>> profileFuture = updateProfileService.toCompletableFuture();
            final CompletableFuture<ResponseBody> emailOptinsFuture = updateEmailOptinsService.toCompletableFuture();
            final CompletableFuture<ResponseBody> postalOptinsFuture = updatePostalOptinsService.toCompletableFuture();
            
            return CompletableFuture.allOf(accountFuture, profileFuture, emailOptinsFuture, postalOptinsFuture)
                    .exceptionally(throwable -> {
                        // if both Guest Account and Profile failed, throw the exception. otherwise,
                        // let the process go through.
                        if (accountFuture.isCompletedExceptionally() && profileFuture.isCompletedExceptionally()) {
                            
                            MiddlewareError.MiddlewareErrorBuilder error = MiddlewareError.builder();
                            
                            error.internalMessage("The service did not complete successfully.");
                            error.developerMessage(throwable.getCause().toString());
                            error.errorCode(MULTIPLE_BACKEND_ERROR);
                            
                            StringBuilder sb = new StringBuilder();
                            sb.append("Update Account and Update Profile services failed. ");
                            
                            if (emailOptinsFuture.isCompletedExceptionally()) {
                                sb.append("Update Email Optins service failed. ");
                            }
                            
                            if (postalOptinsFuture.isCompletedExceptionally()) {
                                sb.append("Update Postal Optins service failed. ");
                            }
                            
                            error.userMessage(sb.toString());
                            
                            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), error.build());
                        }
                        
                        return null;
                    })
                    .thenApply(o -> {
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, enrichedGuest.getVdsId())
                                .ask(new GuestAccountCommand.UpdateGuest(enrichedGuest));
                        
                        ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
                        
                        // If the update occurred successfully...
                        if (!accountFuture.isCompletedExceptionally()) {
                            LOGGER.debug("The account future completed successfully.");
                            
                            // Using the original account information PRIOR to update...
                            originalSaviyntAccount
                                    .exceptionally(throwable -> {
                                        LOGGER.error("The original account retrieval failed.");
                                        throw new MiddlewareTransportException(TransportErrorCode.InternalServerError,
                                                "Retrieving the original account failed: "
                                                        + throwable.getCause().getMessage());
                                    })
                                    .thenAccept(accountInformation -> {
                                        String originalEmail = accountInformation.getGuest().getEmail();
                                        String updatedEmail = enrichedGuest.getEmail();
                                        
                                        LOGGER.debug("Comparing original email to supposedly updated email.");
                                        LOGGER.debug("originalEmail := " + originalEmail);
                                        LOGGER.debug("updatedEmail := " + updatedEmail);
                                        
                                        boolean emailUpdated = false;
                                        
                                        // Check if the email was updated. If so, send the notification.
                                        if (StringUtils.isNoneBlank(originalEmail, updatedEmail)
                                                && !originalEmail.equalsIgnoreCase(updatedEmail)) {
                                            emailUpdatedConfirmationEmail.send(enrichedGuest, languageCode);
                                            emailUpdated = true;
                                        }
                                        
                                        // Check if the password was updated. If so, send the notification.
                                        if (enrichedGuest.getSignInInformation() != null
                                                && enrichedGuest.getSignInInformation().getPassword() != null
                                                && enrichedGuest.getSignInInformation().getPassword().length > 0) {
                                            
                                            LOGGER.info("The password was updated.");
                                            
                                            String email = emailUpdated ? updatedEmail : originalEmail;
                                            String firstName;
                                            
                                            if (enrichedGuest.getPersonalInformation() != null) {
                                                firstName = StringUtils.defaultIfBlank(
                                                        enrichedGuest.getPersonalInformation().getFirstName(),
                                                        accountInformation.getGuest().getFirstName());
                                            } else {
                                                firstName = accountInformation.getGuest().getFirstName();
                                            }
                                            
                                            passwordUpdatedConfirmationEmail.send(email, firstName,
                                                    enrichedGuest.getHeader(), languageCode);
                                        }
                                    });
                        }
                        
                        if (accountFuture.isCompletedExceptionally()
                                || profileFuture.isCompletedExceptionally()
                                || emailOptinsFuture.isCompletedExceptionally()
                                || postalOptinsFuture.isCompletedExceptionally()) {
                            objectNode.put("status", "The service completed with some exceptions.");
                            objectNode.put("updateAccount",
                                    "completed= " + !accountFuture.isCompletedExceptionally());
                            objectNode.put("updateProfile",
                                    "completed= " + !profileFuture.isCompletedExceptionally());
                            objectNode.put("updateEmailOptins",
                                    "completed= " + !emailOptinsFuture.isCompletedExceptionally());
                            objectNode.put("updatePostalOptins",
                                    "completed= " + !emailOptinsFuture.isCompletedExceptionally());
                            
                            return Pair.create(ResponseHeader.OK, ResponseBody
                                    .<JsonNode>builder()
                                    .status(ResponseHeader.OK.status())
                                    .payload(objectNode)
                                    .build());
                        } else {
                            return Pair.create(ResponseHeader.OK, ResponseBody
                                    .<JsonNode>builder()
                                    .status(ResponseHeader.OK.status())
                                    .payload(TextNode.valueOf(enrichedGuest.getVdsId()))
                                    .build());
                        }
                    });
        };
    }
    
    /**
     * Retrieves guest account information from Saviynt with the given VDS ID.
     *
     * @param vdsId the guest account's VDS ID.
     * @return {@link Guest} guest account information from VDS.
     */
    private ServiceCall<NotUsed, Guest> getAccount(String vdsId) {
        return notUsed -> {
            if (StringUtils.isBlank(vdsId)) {
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422),
                        "VDS ID is required.", CONSTRAINT_VIOLATION);
            }
            
            return saviyntService.getGuestAccount("systemUserName", Optional.empty(), Optional.of(vdsId)).invoke()
                    .exceptionally(throwable -> {
                        LOGGER.error("Saviynt GET Guest Account failed.", throwable);
                        Throwable cause = throwable.getCause();
                        if (cause instanceof ConnectException
                                || cause instanceof SaviyntExceptionFactory.SaviyntEnvironmentException) {
                            throw new MiddlewareTransportException(TransportErrorCode.ServiceUnavailable,
                                    throwable.getMessage(), UNKNOWN_ERROR);
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                            throw new GuestNotFoundException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.InternalServerError,
                                throwable.getMessage(), UNKNOWN_ERROR);
                    })
                    .thenApply(Mapper::mapSaviyntGuestToGuest);
        };
    }
    
    /**
     * Update Account service processing for Saviynt which is not exposed as service endpoint.
     *
     * @return {@link HeaderServiceCall} with {@link Pair}
     */
    private ServiceCall<Guest, NotUsed> updateAccount() {
        return guest -> {
            MiddlewareValidation.validateWithGroups(guest, CONSTRAINT_VIOLATION, Guest.UpdateChecks.class);
            
            return this.verifyUpdateAccountLoyaltyInformation(guest)
                    .thenComposeAsync(updatedGuest -> {
                        final SaviyntGuest saviyntGuest = Mapper.mapGuestToSaviyntGuest(updatedGuest, false).build();
                        
                        // If password is provided then invoke Saviynt changePassword
                        // and pass pwdreset = false in updateUser call.
                        CompletionStage<GenericSaviyntResponse> updatePasswordService = null;
                        if (updatedGuest.getPassword() != null) {
                            SaviyntAuthAccountPassword updatePassword = SaviyntAuthAccountPassword
                                    .builder()
                                    .password(updatedGuest.getPassword())
                                    .vdsId(updatedGuest.getVdsId())
                                    .build();
                            
                            updatePasswordService = saviyntService.updateAuthenticatedAccountPassword()
                                    .invoke(updatePassword)
                                    .exceptionally(throwable -> {
                                        Throwable cause = throwable.getCause();
                                        LOGGER.error("Error encountered while trying to update "
                                                        + "account password for VDS ID={}",
                                                saviyntGuest.getVdsId(), throwable);
                                        
                                        if (cause instanceof ConnectException || cause
                                                instanceof SaviyntExceptionFactory.SaviyntEnvironmentException) {
                                            throw new MiddlewareTransportException(TransportErrorCode
                                                    .ServiceUnavailable, throwable.getMessage(), UNKNOWN_ERROR);
                                        } else if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                                            throw new GuestNotFoundException();
                                        } else if (cause instanceof SaviyntExceptionFactory
                                                .InvalidPasswordFormatException) {
                                            throw new InvalidPasswordException();
                                        } else if (cause instanceof SaviyntExceptionFactory.PasswordReuseException) {
                                            throw new InvalidPasswordException(InvalidPasswordException.REUSE_ERROR);
                                        }
                                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500),
                                                throwable.getCause().getMessage(), UNKNOWN_ERROR);
                                    });
                        }
                        
                        if (updatePasswordService != null) {
                            return updatePasswordService.thenCompose(changePasswordResponse ->
                                    this.callSaviyntUpdateGuestAccount(saviyntGuest)
                                            .thenApply(notUsed -> NotUsed.getInstance()));
                        } else {
                            return this.callSaviyntUpdateGuestAccount(saviyntGuest)
                                    .thenApply(notUsed -> NotUsed.getInstance());
                        }
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, ResponseBody<JsonNode>> validateEmail(String email, Optional<String> inputType) {
        return (requestHeader, notUsed) -> {
            // propertyToSearch is defaulted to displayName if inputType is null or if passed with "username".
            String propertyToSearch = inputType.isPresent()
                    && inputType.get().equalsIgnoreCase("email") ? "email" : "displayname";
            
            if ("email".equalsIgnoreCase(propertyToSearch)) {
                Pattern pattern = Pattern.compile(ValidatorConstants.EMAIL_REGEXP);
                Matcher matcher = pattern.matcher(email);
                
                if (!matcher.matches()) {
                    throw new InvalidEmailFormatException();
                }
            }
            
            return saviyntService.getAccountStatus(email, propertyToSearch, "False").invoke()
                    .exceptionally(throwable -> {
                        Throwable cause = throwable.getCause();
                        
                        LOGGER.error("Error encountered while validating account status for given = {}",
                                email, throwable);
                        
                        if (cause instanceof ConnectException
                                || cause instanceof SaviyntExceptionFactory.SaviyntEnvironmentException) {
                            throw new MiddlewareTransportException(TransportErrorCode.ServiceUnavailable,
                                    throwable.getMessage(), UNKNOWN_ERROR);
                        }
                        
                        // in case of non existing account, return an AccountStatus with DoesNotExist message instead.
                        // So that the service will return a 200 with a status of "DoesNotExist"
                        if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                            return AccountStatus.builder()
                                    .message(AccountStatusEnum.DOES_NOT_EXIST.value())
                                    .build();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidEmailFormatException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500),
                                cause.getMessage(), UNKNOWN_ERROR);
                    })
                    .thenApply(accountStatus -> {
                        ObjectNode response = OBJECT_MAPPER.createObjectNode();
                        AccountStatusEnum accountStatusEnum = AccountStatusEnum.fromValue(accountStatus.getMessage());
                        
                        String status;
                        if (accountStatus.getIsAccountLocked() != null && accountStatus.getIsAccountLocked()) {
                            status = AccountStatusEnum.LOCKED.value();
                        } else if (StringUtils.isNotBlank(accountStatus.getMessage()) && accountStatusEnum != null) {
                            status = accountStatusEnum.value();
                        } else {
                            status = AccountStatusEnum.DOES_NOT_EXIST.value();
                        }
                        
                        return Pair.create(ResponseHeader.OK, ResponseBody
                                .<JsonNode>builder()
                                .status(ResponseHeader.OK.status())
                                .payload(response.put("status", status))
                                .build());
                    });
        };
    }
    
    public Topic<GuestEvent> linkLoyaltyTopic() {
        return TopicProducer.taggedStreamWithOffset(GuestAccountTag.GUEST_ACCOUNT_EVENT_TAG.allTags(), (tag, offset) ->
                persistentEntityRegistry.eventStream(tag, offset)
                        .filter(param -> param.first() instanceof GuestAccountEvent.GuestCreated
                                || param.first() instanceof GuestAccountEvent.GuestUpdated)
                        .mapAsync(1, eventOffset -> {
                            LOGGER.debug("Publishing link loyalty message...");
                            GuestAccountEvent event = eventOffset.first();
                            GuestEvent guestEvent;
                            if (event instanceof GuestAccountEvent.GuestCreated) {
                                GuestAccountEvent.GuestCreated eventInstance = (GuestAccountEvent.GuestCreated) event;
                                guestEvent = new GuestEvent.AccountCreated(eventInstance.getGuest());
                                
                            } else {
                                GuestAccountEvent.GuestUpdated eventInstance = (GuestAccountEvent.GuestUpdated) event;
                                guestEvent = new GuestEvent.AccountUpdated(eventInstance.getEnrichedGuest());
                            }
                            
                            return CompletableFuture.completedFuture(new Pair<>(guestEvent, eventOffset.second()));
                        }));
    }
    
    @Override
    public Topic<EnrichedGuest> verifyLoyaltyTopic() {
        return TopicProducer.taggedStreamWithOffset(GuestAccountTag.GUEST_ACCOUNT_EVENT_TAG.allTags(), (tag, offset) ->
                persistentEntityRegistry.eventStream(tag, offset)
                        .filter(param -> param.first() instanceof GuestAccountEvent.VerifyLoyalty)
                        .mapAsync(1, eventOffset -> {
                            LOGGER.debug("Publishing verify loyalty message...");
                            GuestAccountEvent event = eventOffset.first();
                            GuestAccountEvent.VerifyLoyalty loyalty = (GuestAccountEvent.VerifyLoyalty) event;
                            
                            return CompletableFuture.completedFuture(
                                    new Pair<>(loyalty.getEnrichedGuest(), eventOffset.second()));
                        }));
    }
    
    @Override
    public Topic<EmailNotification> emailNotificationTopic() {
        return TopicProducer.singleStreamWithOffset(offset ->
                persistentEntityRegistry
                        .eventStream(EmailNotificationTag.EMAIL_NOTIFICATION_TAG, offset)
                        .map(pair -> {
                            LOGGER.debug("Publishing email notification message...");
                            EmailNotification eventNotification = pair.first().getEmailNotification();
                            EmailNotification emailNotification = EmailNotification
                                    .builder()
                                    .sender(eventNotification.getSender())
                                    .recipient(eventNotification.getRecipient())
                                    .subject(eventNotification.getSubject())
                                    .content(eventNotification.getContent())
                                    .build();
                            return new Pair<>(emailNotification, pair.second());
                        })
        );
    }
    
    /**
     * Triggers a call to Saviynt updateUser service.
     *
     * @param saviyntGuest the {@link SaviyntGuest} request.
     * @return {@link CompletionStage}<{@link GenericSaviyntResponse}>
     */
    private CompletionStage<GenericSaviyntResponse> callSaviyntUpdateGuestAccount(SaviyntGuest saviyntGuest) {
        return saviyntService.updateGuestAccount()
                .invoke(saviyntGuest)
                .exceptionally(exception -> {
                    Throwable cause = exception.getCause();
                    
                    LOGGER.error("Error encountered while trying to update account for VDS ID={}",
                            saviyntGuest.getVdsId(), exception);
                    
                    if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                        throw new GuestNotFoundException();
                    }
                    
                    if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                        throw new InvalidEmailFormatException();
                    }
                    
                    if (cause instanceof SaviyntExceptionFactory.InvalidPasswordFormatException) {
                        throw new InvalidPasswordException();
                    }
                    
                    throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500),
                            cause.getMessage(), UNKNOWN_ERROR);
                });
    }
    
    /**
     * Verifies if any loyalty ID is specified in the request. If so, retrieves the guest record
     * from VDS via {@link SaviyntService} {@code getUser}. If any number is set to zero (0), will
     * then set the {@link Guest} request object's loyalty IDs to empty strings so that the {@code updateUser}
     * invocation will empty out the loyalty IDs in VDS. This is implemented to show the "on processing" state
     * of VDS to Siebel validation and synchronization.
     *
     * @param guest the {@link Guest} request object from service invocation.
     * @return {@link CompletionStage} of updated {@link Guest} request object.
     */
    private CompletionStage<Guest> verifyUpdateAccountLoyaltyInformation(Guest guest) {
        if (StringUtils.isNotBlank(guest.getCrownAndAnchorId())
                || StringUtils.isNotBlank(guest.getCaptainsClubId())
                || StringUtils.isNotBlank(guest.getAzamaraLoyaltyId())
                || StringUtils.isNotBlank(guest.getCelebrityBlueChipId())
                || StringUtils.isNotBlank(guest.getClubRoyaleId())) {
            
            return saviyntService
                    .getGuestAccount("systemUserName", Optional.empty(), Optional.of(guest.getVdsId()))
                    .invoke()
                    .exceptionally(throwable -> {
                        LOGGER.error("Failed to retrieve guest account with VDS ID {} for Loyalty check.",
                                guest.getVdsId(), throwable);
                        return null;
                    })
                    .thenApply(accInfo -> {
                        Guest.GuestBuilder guestBuilder = Guest.builder();
                        if (accInfo != null) {
                            SaviyntGuest savGuest = accInfo.getGuest();
                            if ("0".equals(savGuest.getCrownAndAnchorId())) {
                                guestBuilder.crownAndAnchorId("");
                            }
                            
                            if ("0".equals(savGuest.getCaptainsClubId())) {
                                guestBuilder.captainsClubId("");
                            }
                            
                            if ("0".equals(savGuest.getAzamaraLoyaltyId())) {
                                guestBuilder.azamaraLoyaltyId("");
                            }
                            
                            if ("0".equals(savGuest.getCelebrityBlueChipId())) {
                                guestBuilder.celebrityBlueChipId("");
                            }
                            
                            if ("0".equals(savGuest.getClubRoyaleId())) {
                                guestBuilder.clubRoyaleId("");
                            }
                        }
                        return Mapper.mapCurrentGuestToUpdatedGuest(guest, guestBuilder.build());
                    });
        } else {
            return CompletableFuture.completedFuture(guest);
        }
    }
    
    /**
     * Executes {@link CompletionStage} for both Email and Postal optins if required parameters are available and
     * creates a {@link Pair} that contains both, at least one of them, or none (null).
     *
     * @param guest  The {@link Guest} object.
     * @param appKey The AppKey from request headers.
     * @return {@link CompletionStage} which contains a {@link Pair} of Email and Postal {@link ResponseBody}.
     */
    private CompletionStage<Pair<ResponseBody<EmailOptins>, ResponseBody<PostalOptins>>> getOptins(Guest guest,
                                                                                                   String appKey) {
        CompletionStage<ResponseBody<EmailOptins>> emailOptinStage = null;
        if (guest != null && StringUtils.isNotBlank(guest.getEmail())) {
            emailOptinStage = guestProfileOptinService.getEmailOptins(guest.getEmail())
                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                    .invoke()
                    .exceptionally(throwable -> {
                        LOGGER.error("GET Email Optins failed.", throwable);
                        return null;
                    });
        }
        
        CompletionStage<ResponseBody<PostalOptins>> postalOptinStage = null;
        if (guest != null && StringUtils.isNotBlank(guest.getVdsId())) {
            postalOptinStage = guestProfileOptinService.getPostalOptins(guest.getVdsId())
                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                    .invoke()
                    .exceptionally(throwable -> {
                        LOGGER.error("GET Postal Optins failed.", throwable);
                        return null;
                    });
        }
        
        if (emailOptinStage != null && postalOptinStage != null) {
            return emailOptinStage.thenCombineAsync(postalOptinStage, Pair::create);
        }
        
        if (emailOptinStage != null) {
            return emailOptinStage.thenApply(emailResponse -> Pair.create(emailResponse, null));
        }
        
        if (postalOptinStage != null) {
            return postalOptinStage.thenApply(postalResponse -> Pair.create(null, postalResponse));
        }
        
        return CompletableFuture.completedFuture(Pair.create(null, null));
    }
    
    /**
     * Filters an object based on the {@code @JsonView} annotation value placed on the attributes.
     *
     * @param source   the object where the filter will be applied.
     * @param jsonView the {@link JsonView} to be applied.
     * @param <T>      the class type of source passed in the argument.
     * @return T filtered source object.
     */
    @SuppressWarnings("unchecked")
    private <T> T filterObjectView(T source, Class<?> jsonView) {
        try {
            String filteredString = OBJECT_MAPPER
                    .writerWithView(jsonView)
                    .writeValueAsString(source);
            return OBJECT_MAPPER.readValue(filteredString, (Class<T>) source.getClass());
        } catch (Exception e) {
            LOGGER.error("Failed to process JsonView.", e);
        }
        
        return source;
    }
}
