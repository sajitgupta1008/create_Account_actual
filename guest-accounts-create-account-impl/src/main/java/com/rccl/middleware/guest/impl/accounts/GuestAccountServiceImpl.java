package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.japi.Pair;
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
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
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
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.guest.authentication.AccountCredentials;
import com.rccl.middleware.guest.authentication.GuestAuthenticationService;
import com.rccl.middleware.guest.impl.accounts.email.AccountCreatedConfirmationEmail;
import com.rccl.middleware.guest.impl.accounts.email.EmailNotificationEntity;
import com.rccl.middleware.guest.impl.accounts.email.EmailNotificationTag;
import com.rccl.middleware.guest.impl.accounts.email.EmailUpdatedConfirmationEmail;
import com.rccl.middleware.guest.impl.accounts.email.PasswordUpdatedConfirmationEmail;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guest.optin.OptinType;
import com.rccl.middleware.guest.optin.Optins;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.guestprofiles.models.Profile;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import com.rccl.middleware.saviynt.api.requests.SaviyntGuest;
import com.rccl.middleware.saviynt.api.responses.AccountInformation;
import com.rccl.middleware.saviynt.api.responses.AccountStatus;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import ch.qos.logback.classic.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(GuestAccountServiceImpl.class);
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String APPKEY_HEADER = "AppKey";
    
    private static final String DEFAULT_APP_KEY = ConfigFactory.load().getString("apigee.appkey");
    
    private final AccountCreatedConfirmationEmail accountCreatedConfirmationEmail;
    
    private final EmailUpdatedConfirmationEmail emailUpdatedConfirmationEmail;
    
    private final PasswordUpdatedConfirmationEmail passwordUpdatedConfirmationEmail;
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final SaviyntService saviyntService;
    
    private final GuestProfilesService guestProfilesService;
    
    private final GuestProfileOptinService guestProfileOptinService;
    
    private final GuestAuthenticationService guestAuthenticationService;
    
    @Inject
    public GuestAccountServiceImpl(AccountCreatedConfirmationEmail accountCreatedConfirmationEmail,
                                   EmailUpdatedConfirmationEmail emailUpdatedConfirmationEmail,
                                   PasswordUpdatedConfirmationEmail passwordUpdatedConfirmationEmail,
                                   SaviyntService saviyntService,
                                   PersistentEntityRegistry persistentEntityRegistry,
                                   GuestProfilesService guestProfilesService,
                                   GuestProfileOptinService guestProfileOptinService,
                                   GuestAuthenticationService guestAuthenticationService) {
        
        this.saviyntService = saviyntService;
        
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
            MiddlewareValidation.validateWithGroups(guest, Guest.CreateChecks.class);
            
            final SaviyntGuest saviyntGuest = Mapper.mapGuestToSaviyntGuest(guest, true).build();
            
            return saviyntService
                    .createGuestAccount()
                    .invoke(saviyntGuest)
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof SaviyntExceptionFactory.ExistingGuestException) {
                            throw new ExistingGuestException();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidGuestException("The email is in an invalid format.", null);
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidPasswordFormatException) {
                            throw InvalidGuestException.INVALID_PASSWORD;
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenCompose(response -> {
                        String message = response.getMessage();
                        Pattern pattern = Pattern.compile("vdsid=[a-zA-Z0-9]*");
                        Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        
                        String vdsId = matcher.group(0).substring(6);
                        
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(Mapper.mapVdsIdWithGuest(vdsId, guest)));
                        
                        String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
                        
                        // trigger optin service to store the optins into Cassandra
                        if (!CollectionUtils.isEmpty(guest.getOptins())) {
                            guestProfileOptinService.createOptins(guest.getEmail())
                                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                                    .invoke(this.generateCreateOptinsRequest(guest))
                                    .toCompletableFuture().complete(ResponseBody.builder().build());
                        }
                        
                        if ("web".equals(guest.getHeader().getChannel())) {
                            ObjectNode objNode = OBJECT_MAPPER.createObjectNode();
                            objNode.put("vdsId", vdsId);
                            
                            // Send the account created confirmation email.
                            accountCreatedConfirmationEmail.send(guest);
                            
                            return CompletableFuture.completedFuture(
                                    Pair.create(ResponseHeader.OK.withStatus(201), ResponseBody
                                            .<JsonNode>builder()
                                            .status(201)
                                            .payload(objNode)
                                            .build()));
                        } else {
                            // automatically authenticate user and include vdsId in the response.
                            return guestAuthenticationService.authenticateUser()
                                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                                    .invoke(AccountCredentials.builder()
                                            .header(guest.getHeader())
                                            .username(guest.getEmail())
                                            .password(guest.getPassword())
                                            .build())
                                    .thenApply(authResponse -> {
                                        // Send the account created confirmation email.
                                        accountCreatedConfirmationEmail.send(guest);
                                        
                                        return Pair.create(ResponseHeader.OK.withStatus(201), authResponse);
                                    });
                        }
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, ResponseBody<EnrichedGuest>> getAccountEnriched(String vdsId) {
        return (requestHeader, notUsed) -> {
            
            if (StringUtils.isBlank(vdsId)) {
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422), "VDS ID is required.");
            }
            
            String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
            
            // In case of exception, return null and let the other process go through to return whichever
            // attributes are available.
            final CompletionStage<ResponseBody<Profile>> getProfile = guestProfilesService.getProfile(vdsId)
                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                    .invoke().exceptionally(throwable -> null);
            
            return this.getAccount(vdsId).invoke().exceptionally(throwable -> null)
                    .thenCombineAsync(getProfile, (guest, profile) -> {
                        ResponseBody<Optins> optins = null;
                        
                        if (guest != null && StringUtils.isNotBlank(guest.getEmail())) {
                            optins = guestProfileOptinService.getOptins(guest.getEmail())
                                    .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey))
                                    .invoke()
                                    .exceptionally(throwable -> null)
                                    .toCompletableFuture()
                                    .join();
                        }
                        
                        if (guest == null && profile == null) {
                            throw new GuestNotFoundException();
                        }
                        
                        EnrichedGuest enrichedGuest = Mapper.mapToEnrichedGuest(guest, profile, optins);
                        return Pair.create(ResponseHeader.OK, ResponseBody
                                .<EnrichedGuest>builder()
                                .status(ResponseHeader.OK.status())
                                .payload(enrichedGuest)
                                .build());
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
            
            MiddlewareValidation.validate(enrichedGuest);
            
            String appKey = requestHeader.getHeader(APPKEY_HEADER).orElse(DEFAULT_APP_KEY);
            
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
            
            if (!profileBuilder.build().equals(Profile.builder().build())) {
                final Profile profile = profileBuilder.vdsId(enrichedGuest.getVdsId()).build();
                updateProfileService = guestProfilesService.updateProfile()
                        .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey)).invoke(profile);
            }
            
            CompletionStage<ResponseBody> updateOptinsService = CompletableFuture
                    .completedFuture(ResponseBody.builder().build());
            Optins optins = Mapper.mapEnrichedGuestToOptins(enrichedGuest);
            
            if (optins != null && StringUtils.isNotBlank(enrichedGuest.getEmail())) {
                updateOptinsService = guestProfileOptinService
                        .updateOptins(enrichedGuest.getEmail())
                        .handleRequestHeader(rh -> rh.withHeader(APPKEY_HEADER, appKey)).invoke(optins);
            }
            
            final CompletableFuture<NotUsed> accountFuture = updateAccountService.toCompletableFuture();
            final CompletableFuture<ResponseBody<TextNode>> profileFuture = updateProfileService.toCompletableFuture();
            final CompletableFuture<ResponseBody> optinsFuture = updateOptinsService.toCompletableFuture();
            
            return CompletableFuture.allOf(accountFuture, profileFuture, optinsFuture)
                    .exceptionally(throwable -> {
                        // if both Guest Account and Profile failed, throw the exception. otherwise,
                        // let the process go through.
                        if (accountFuture.isCompletedExceptionally() && profileFuture.isCompletedExceptionally()) {
                            
                            MiddlewareError.MiddlewareErrorBuilder error = MiddlewareError.builder();
                            
                            error.internalMessage("The service did not complete successfully.");
                            error.developerMessage(throwable.getCause().toString());
                            
                            StringBuilder sb = new StringBuilder();
                            sb.append("Update Account and Update Profile services failed. ");
                            
                            if (optinsFuture.isCompletedExceptionally()) {
                                sb.append("Update Optins service failed. ");
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
                            LOGGER.info("The account future completed successfully.");
                            
                            // Using the original account information PRIOR to update...
                            originalSaviyntAccount
                                    .thenAccept(accountInformation -> {
                                        String originalEmail = accountInformation.getGuest().getEmail();
                                        String updatedEmail = enrichedGuest.getEmail();
                                        
                                        LOGGER.info("Comparing original email to supposedly updated email.");
                                        LOGGER.info("originalEmail := " + originalEmail);
                                        LOGGER.info("updatedEmail := " + updatedEmail);
                                        
                                        // Check if the email was updated. If so, send the notification.
                                        if (StringUtils.isNoneBlank(originalEmail, updatedEmail)
                                                && !originalEmail.equalsIgnoreCase(updatedEmail)) {
                                            emailUpdatedConfirmationEmail.send(enrichedGuest);
                                        }
                                        
                                        LOGGER.info("The password was updated.");
                                        
                                        // Check if the password was updated. If so, send the notification.
                                        if (!ArrayUtils.isEmpty(enrichedGuest.getSignInInformation().getPassword())) {
                                            passwordUpdatedConfirmationEmail.send(enrichedGuest.getEmail(),
                                                    enrichedGuest.getPersonalInformation().getFirstName(),
                                                    enrichedGuest.getHeader());
                                        }
                                    })
                                    .exceptionally(throwable -> {
                                        LOGGER.error("The original account retrieval failed.");
                                        throw new MiddlewareTransportException(TransportErrorCode.InternalServerError,
                                                "Retrieving the original account failed: "
                                                        + throwable.getCause().getMessage());
                                    });
                        }
                        
                        if (accountFuture.isCompletedExceptionally() || profileFuture.isCompletedExceptionally()
                                || optinsFuture.isCompletedExceptionally()) {
                            objectNode.put("status", "The service completed with some exceptions.");
                            objectNode.put("updateAccount",
                                    "completed= " + !accountFuture.isCompletedExceptionally());
                            objectNode.put("updateProfile",
                                    "completed= " + !profileFuture.isCompletedExceptionally());
                            objectNode.put("updateOptins",
                                    "completed= " + !optinsFuture.isCompletedExceptionally());
                            
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
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422), "VDS ID is required.");
            }
            
            return saviyntService.getGuestAccount("systemUserName", Optional.empty(), Optional.of(vdsId)).invoke()
                    .exceptionally(throwable -> {
                        Throwable cause = throwable.getCause();
                        if (cause instanceof SaviyntExceptionFactory.ExistingGuestException
                                || cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                            throw new GuestNotFoundException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.BadRequest, throwable);
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
            
            MiddlewareValidation.validateWithGroups(guest, Guest.UpdateChecks.class);
            
            final SaviyntGuest saviyntGuest = Mapper.mapGuestToSaviyntGuest(guest, false).build();
            
            return saviyntService
                    .updateGuestAccount()
                    .invoke(saviyntGuest)
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException
                                || cause instanceof SaviyntExceptionFactory.ExistingGuestException) {
                            throw new GuestNotFoundException();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidGuestException("The email is in an invalid format.", null);
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidPasswordFormatException) {
                            throw InvalidGuestException.INVALID_PASSWORD;
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(response -> NotUsed.getInstance());
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, ResponseBody<JsonNode>> validateEmail(String email) {
        return (requestHeader, notUsed) -> {
            
            Pattern pattern = Pattern.compile(ValidatorConstants.EMAIL_REGEXP);
            Matcher matcher = pattern.matcher(email);
            
            if (!matcher.matches()) {
                throw new InvalidEmailFormatException();
            }
            
            return saviyntService.getAccountStatus(email, "displayName", "False").invoke()
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        // in case of non existing account, return an AccountStatus with DoesNotExist message instead.
                        // So that the service will return a 200 with a status of "DoesNotExist"
                        if (cause instanceof SaviyntExceptionFactory.ExistingGuestException) {
                            return AccountStatus.builder()
                                    .message(AccountStatusEnum.DOES_NOT_EXIST.value())
                                    .build();
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidEmailFormatException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(accountStatus -> {
                        ObjectNode response = OBJECT_MAPPER.createObjectNode();
                        AccountStatusEnum accountStatusEnum = AccountStatusEnum.fromValue(accountStatus.getMessage());
                        
                        String status;
                        
                        if (StringUtils.isNotBlank(accountStatus.getMessage()) && accountStatusEnum != null) {
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
    
    @Override
    public HeaderServiceCall<NotUsed, String> healthCheck() {
        return (requestHeader, request) -> {
            String quote = "Here's to tall ships. "
                    + "Here's to small ships. "
                    + "Here's to all the ships on the sea. "
                    + "But the best ships are friendships, so here's to you and me!";
            
            return CompletableFuture.completedFuture(new Pair<>(ResponseHeader.OK, quote));
        };
    }
    
    public Topic<GuestEvent> linkLoyaltyTopic() {
        return TopicProducer.taggedStreamWithOffset(GuestAccountTag.GUEST_ACCOUNT_EVENT_TAG.allTags(), (tag, offset) ->
                persistentEntityRegistry.eventStream(tag, offset)
                        .filter(param -> param.first() instanceof GuestAccountEvent.GuestCreated
                                || param.first() instanceof GuestAccountEvent.GuestUpdated)
                        .mapAsync(1, eventOffset -> {
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
     * Populates {@link Optins} to register the guest email to all brands and all categories of optins specified in
     * create account request.
     *
     * @param guest the {@link Guest} model
     * @return {@link Optins} with enrollment to all brands and all optin categories.
     */
    
    private Optins generateCreateOptinsRequest(Guest guest) {
        List<OptinType> optinTypeList = new ArrayList<>();
        guest.getOptins().forEach(optin ->
                optinTypeList.add(OptinType.builder()
                        .type(optin.getType())
                        .acceptTime(optin.getAcceptTime())
                        .flag(optin.getFlag())
                        .build()));
        
        // enroll the guest to all brands and categories.
        List<Optin> optinList = new ArrayList<>();
        Arrays.asList('R', 'C', 'Z').forEach(brand ->
                Arrays.asList("marketing", "operational").forEach(category ->
                        optinList.add(Optin.builder()
                                .brand(brand)
                                .category(category)
                                .types(optinTypeList)
                                .build())
                )
        );
        
        return Optins.builder()
                .header(guest.getHeader())
                .email(guest.getEmail())
                .optins(optinList)
                .build();
    }
}
