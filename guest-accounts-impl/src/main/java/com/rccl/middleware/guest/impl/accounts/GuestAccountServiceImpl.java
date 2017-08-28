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
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.validation.MiddlewareValidation;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.forgerock.api.ForgeRockService;
import com.rccl.middleware.forgerock.api.exceptions.ForgeRockExceptionFactory;
import com.rccl.middleware.forgerock.api.jwt.ForgeRockJWTDecoder;
import com.rccl.middleware.forgerock.api.jwt.OpenIdTokenInformation;
import com.rccl.middleware.forgerock.api.requests.ForgeRockCredentials;
import com.rccl.middleware.forgerock.api.requests.LoginStatusEnum;
import com.rccl.middleware.forgerock.api.responses.MobileAuthenticationTokens;
import com.rccl.middleware.guest.accounts.AccountCredentials;
import com.rccl.middleware.guest.accounts.AccountStatusEnum;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.exceptions.ExistingGuestException;
import com.rccl.middleware.guest.accounts.exceptions.GuestAuthenticationException;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidEmailFormatException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guest.optin.OptinType;
import com.rccl.middleware.guest.optin.Optins;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.guestprofiles.models.Profile;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import com.rccl.middleware.saviynt.api.requests.SaviyntGuest;
import com.rccl.middleware.saviynt.api.responses.AccountStatus;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final SaviyntService saviyntService;
    
    private final ForgeRockService forgeRockService;
    
    private final GuestProfilesService guestProfilesService;
    
    private final GuestProfileOptinService guestProfileOptinService;
    
    @Inject
    public GuestAccountServiceImpl(SaviyntService saviyntService,
                                   ForgeRockService forgeRockService,
                                   PersistentEntityRegistry persistentEntityRegistry,
                                   GuestProfilesService guestProfilesService,
                                   GuestProfileOptinService guestProfileOptinService) {
        this.saviyntService = saviyntService;
        this.forgeRockService = forgeRockService;
        
        this.guestProfilesService = guestProfilesService;
        this.guestProfileOptinService = guestProfileOptinService;
        
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(GuestAccountEntity.class);
    }
    
    @Override
    public HeaderServiceCall<Guest, JsonNode> createAccount() {
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
                        
                        // TODO: Replace this with the vdsId attribute when available.
                        String message = response.getMessage();
                        Pattern pattern = Pattern.compile("vdsid=[a-zA-Z0-9]*");
                        Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        
                        String vdsId = matcher.group(0).substring(6);
                        
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(Mapper.mapVdsIdWithGuest(vdsId, guest)));
                        
                        // trigger optin service to store the optins into Cassandra
                        guestProfileOptinService.createOptins(guest.getEmail())
                                .invoke(this.generateCreateOptinsRequest(guest))
                                .toCompletableFuture().complete(NotUsed.getInstance());
                        
                        if ("web".equals(guest.getHeader().getChannel())) {
                            ObjectNode objNode = OBJECT_MAPPER.createObjectNode();
                            objNode.put("vdsId", vdsId);
                            
                            return CompletableFuture.completedFuture(
                                    Pair.create(ResponseHeader.OK.withStatus(201), objNode));
                            
                        } else {
                            // automatically authenticate user and include vdsId in the response.
                            AccountCredentials credentials = AccountCredentials.builder()
                                    .header(guest.getHeader())
                                    .username(guest.getEmail())
                                    .password(guest.getPassword())
                                    .build();
                            
                            return this.authenticateUser()
                                    .invokeWithHeaders(requestHeader, credentials)
                                    .thenApply(pair ->
                                            Pair.create(ResponseHeader.OK.withStatus(201), pair.second()));
                        }
                        
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, EnrichedGuest> getAccountEnriched(String vdsId) {
        return (requestHeader, notUsed) -> {
            
            if (StringUtils.isBlank(vdsId)) {
                throw new MiddlewareTransportException(TransportErrorCode.fromHttp(422), "VDS ID is required.");
            }
            
            // In case of exception, return null and let the other process go through to return whichever
            // attributes are available.
            final CompletionStage<Profile> getProfile = guestProfilesService.getProfile(vdsId)
                    .invoke().exceptionally(throwable -> null);
            
            return this.getAccount(vdsId).invoke().exceptionally(throwable -> null)
                    .thenCombineAsync(getProfile, (guest, profile) -> {
                        Optins optins = null;
                        
                        if (guest != null) {
                            optins = guestProfileOptinService.getOptins(guest.getEmail())
                                    .invoke()
                                    .exceptionally(throwable -> null)
                                    .toCompletableFuture()
                                    .join();
                        }
                        
                        EnrichedGuest enrichedGuest = Mapper.mapToEnrichedGuest(guest, profile, optins);
                        return Pair.create(ResponseHeader.OK, enrichedGuest);
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<EnrichedGuest, JsonNode> updateAccountEnriched() {
        return (requestHeader, enrichedGuest) -> {
            
            MiddlewareValidation.validate(enrichedGuest);
            
            CompletionStage<NotUsed> updateAccountService = CompletableFuture.completedFuture(NotUsed.getInstance());
            Guest.GuestBuilder guestBuilder = Mapper.mapEnrichedGuestToGuest(enrichedGuest);
            
            if (!guestBuilder.build().equals(Guest.builder().build())) {
                final Guest guest = guestBuilder
                        .header(enrichedGuest.getHeader())
                        .vdsId(enrichedGuest.getVdsId())
                        .build();
                updateAccountService = this.updateAccount().invoke(guest);
            }
            
            CompletionStage<TextNode> updateProfileService =
                    CompletableFuture.completedFuture(TextNode.valueOf(enrichedGuest.getVdsId()));
            Profile.ProfileBuilder profileBuilder = Mapper.mapEnrichedGuestToProfile(enrichedGuest);
            
            if (!profileBuilder.build().equals(Profile.builder().build())) {
                final Profile profile = profileBuilder.vdsId(enrichedGuest.getVdsId()).build();
                updateProfileService = guestProfilesService.updateProfile().invoke(profile);
            }
            
            CompletableFuture<NotUsed> updateOptinsService = CompletableFuture.completedFuture(NotUsed.getInstance());
            Optins optins = Mapper.mapEnrichedGuestToOptins(enrichedGuest);
            
            if (optins != null && StringUtils.isNotBlank(enrichedGuest.getEmail())) {
                updateOptinsService = guestProfileOptinService
                        .updateOptins(enrichedGuest.getEmail()).invoke(optins).toCompletableFuture();
            }
            
            final CompletableFuture<NotUsed> accountFuture = updateAccountService.toCompletableFuture();
            final CompletableFuture<TextNode> profileFuture = updateProfileService.toCompletableFuture();
            final CompletableFuture<NotUsed> optinsFuture = updateOptinsService.toCompletableFuture();
            
            return CompletableFuture.allOf(accountFuture, profileFuture, optinsFuture)
                    .exceptionally(throwable -> {
                        // if both Guest Account and Profile failed, throw the exception. otherwise,
                        // let the process go through.
                        if (accountFuture.isCompletedExceptionally() && profileFuture.isCompletedExceptionally()) {
                            
                            MiddlewareExceptionMessage message = new MiddlewareExceptionMessage();
                            message.setErrorMessage("The service did not complete successfully.");
                            message.setDeveloperMessage(throwable.getCause().toString());
                            
                            StringBuilder sb = new StringBuilder();
                            sb.append("Update Account and Update Profile services failed. ");
                            
                            if (optinsFuture.isCompletedExceptionally()) {
                                sb.append("Update Optins service failed. ");
                            }
                            message.setAdditionalInformation(sb.toString());
                            
                            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), message);
                        }
                        
                        return null;
                    })
                    .thenApply(o -> {
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, enrichedGuest.getVdsId())
                                .ask(new GuestAccountCommand.UpdateGuest(enrichedGuest));
                        
                        ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
                        
                        if (accountFuture.isCompletedExceptionally() || profileFuture.isCompletedExceptionally()
                                || optinsFuture.isCompletedExceptionally()) {
                            objectNode.put("status", "The service completed with some exceptions.");
                            objectNode.put("updateAccount",
                                    "completed= " + !accountFuture.isCompletedExceptionally());
                            objectNode.put("updateProfile",
                                    "completed= " + !profileFuture.isCompletedExceptionally());
                            objectNode.put("updateOptins",
                                    "completed= " + !optinsFuture.isCompletedExceptionally());
                            
                            return Pair.create(ResponseHeader.OK, objectNode);
                        } else {
                            return Pair.create(ResponseHeader.OK, TextNode.valueOf(enrichedGuest.getVdsId()));
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
    public HeaderServiceCall<AccountCredentials, JsonNode> authenticateUser() {
        return (requestHeader, request) -> {
            
            if ("web".equals(request.getHeader().getChannel())) {
                throw new GuestAuthenticationException("The channel provided is not allowed to access this service.");
            }
            
            MiddlewareValidation.validate(request);
            
            ForgeRockCredentials forgeRockCredentials = ForgeRockCredentials.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();
            
            return forgeRockService.authenticateMobileUser()
                    .invoke(forgeRockCredentials)
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof ForgeRockExceptionFactory.AuthenticationException) {
                            ForgeRockExceptionFactory.AuthenticationException ex =
                                    (ForgeRockExceptionFactory.AuthenticationException) cause;
                            
                            // if the error description contains "shopperid", then decrypt the message to
                            // get the webshopper information
                            if (StringUtils.contains(ex.getErrorDescription(), "shopperid")) {
                                try {
                                    String decodedString = URLDecoder.decode(ex.getErrorDescription(), "UTF-8");
                                    Pattern pattern = Pattern.compile("\\{.*\\}");
                                    Matcher matcher = pattern.matcher(decodedString);
                                    matcher.find();
                                    
                                    return OBJECT_MAPPER.readValue(matcher.group(0), MobileAuthenticationTokens.class);
                                    
                                } catch (Exception e) {
                                    throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), e);
                                }
                            }
                            
                            throw new GuestAuthenticationException(ex.getErrorDescription());
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenCompose(mobileAuthTokens -> {
                        ObjectNode jsonResponse = OBJECT_MAPPER.createObjectNode();
                        final String accountLoginStatus = "accountLoginStatus";
                        
                        if (StringUtils.isNotBlank(mobileAuthTokens.getAccessToken())) {
                            jsonResponse.put(accountLoginStatus,
                                    LoginStatusEnum.NEW_ACCOUNT_AUTHENTICATED.value())
                                    .put("accessToken", mobileAuthTokens.getAccessToken())
                                    .put("refreshToken", mobileAuthTokens.getRefreshToken())
                                    .put("openIdToken", mobileAuthTokens.getIdToken())
                                    .put("tokenExpiration", mobileAuthTokens.getExpiration());
                            
                            OpenIdTokenInformation decryptedInfo = ForgeRockJWTDecoder
                                    .decodeJwtToken(mobileAuthTokens.getIdToken(), OpenIdTokenInformation.class);
                            
                            if (decryptedInfo != null) {
                                jsonResponse.put("vdsId", decryptedInfo.getVdsId())
                                        .put("firstName", decryptedInfo.getFirstName())
                                        .put("lastName", decryptedInfo.getLastName())
                                        .put("email", decryptedInfo.getEmail())
                                        .put("birthdate", decryptedInfo.getBirthdate());
                            }
                            
                        } else if (StringUtils.isNotBlank(mobileAuthTokens.getWebShopperId())) {
                            jsonResponse.put(accountLoginStatus,
                                    LoginStatusEnum.LEGACY_ACCOUNT_VERIFIED.value())
                                    .put("webShopperId", mobileAuthTokens.getWebShopperId())
                                    .put("webShopperUsername", mobileAuthTokens.getWebShopperUsername())
                                    .put("webShopperFirstName", mobileAuthTokens.getWebShopperFirstName())
                                    .put("webShopperLastName", mobileAuthTokens.getWebShopperLastName())
                                    .put("webShopperEmail", mobileAuthTokens.getWebShopperEmail());
                            
                        } else {
                            // in case of temporary password scenario, Saviynt AccountStatus service 
                            // with generateToken=True must be invoked to retrieve all the necessary attributes
                            // for eventually updating the guest password.
                            return saviyntService
                                    .getAccountStatus(request.getUsername(), "email", "True").invoke()
                                    .exceptionally(throwable -> {
                                        throw new MiddlewareTransportException(
                                                TransportErrorCode.fromHttp(500), throwable);
                                    })
                                    .thenApply(accountStatus -> {
                                        jsonResponse.put(accountLoginStatus,
                                                LoginStatusEnum.LEGACY_ACCOUNT_VERIFIED.value())
                                                .put("vdsId", accountStatus.getVdsId())
                                                .put("email", request.getUsername())
                                                .put("token", accountStatus.getToken());
                                        
                                        return Pair.create(ResponseHeader.OK, jsonResponse);
                                    });
                        }
                        
                        return CompletableFuture.completedFuture(
                                Pair.create(ResponseHeader.OK.withStatus(200), jsonResponse));
                    });
            
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, JsonNode> validateEmail(String email) {
        return (requestHeader, notUsed) -> {
            
            Pattern pattern = Pattern.compile(ValidatorConstants.EMAIL_REGEXP);
            Matcher matcher = pattern.matcher(email);
            
            if (!matcher.matches()) {
                throw new InvalidEmailFormatException();
            }
            
            return saviyntService.getAccountStatus(email, "email", "False").invoke()
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
                        
                        return Pair.create(ResponseHeader.OK, response.put("status", status));
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
