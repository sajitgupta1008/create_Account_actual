package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.rccl.middleware.common.exceptions.MiddlewareExceptionMessage;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.validation.MiddlewareValidation;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.forgerock.api.ForgeRockCredentials;
import com.rccl.middleware.forgerock.api.ForgeRockService;
import com.rccl.middleware.forgerock.api.LoginStatusEnum;
import com.rccl.middleware.forgerock.api.exceptions.ForgeRockExceptionFactory;
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
import com.rccl.middleware.saviynt.api.SaviyntGuest;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import org.apache.commons.lang3.StringUtils;
import org.mockito.internal.matchers.Not;

import javax.inject.Inject;
import javax.xml.ws.Response;
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
                        
                        //TODO replace this with the vdsId attribute when available
                        String message = response.get("message").asText();
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
                                    .thenApply(jsonPair -> {
                                        ObjectNode objNode = jsonPair.second().deepCopy();
                                        objNode.put("vdsId", vdsId);
                                        return Pair.create(ResponseHeader.OK.withStatus(201), objNode);
                                    });
                        }
                        
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<EnrichedGuest, JsonNode> updateAccountEnriched() {
        return (requestHeader, enrichedGuest) -> {
            
            MiddlewareValidation.validate(enrichedGuest);
            
            CompletionStage<NotUsed> updateAccountService = CompletableFuture.completedFuture(NotUsed.getInstance());
            Guest.GuestBuilder guestBuilder = Mapper.mapEnrichedGuestToGuest(enrichedGuest);
            
            if (guestBuilder.build().equals(Guest.builder().build())) {
                final Guest guest = guestBuilder
                        .header(enrichedGuest.getHeader())
                        .vdsId(enrichedGuest.getVdsId())
                        .build();
                updateAccountService = this.updateAccount().invoke(guest);
            }
            
            CompletionStage<TextNode> updateProfileService =
                    CompletableFuture.completedFuture(TextNode.valueOf(enrichedGuest.getVdsId()));
            Profile.ProfileBuilder profileBuilder = Mapper.mapEnrichedGuestToProfile(enrichedGuest);
            
            if (profileBuilder.build().equals(Profile.builder().build())) {
                final Profile profile = profileBuilder.vdsId(enrichedGuest.getVdsId()).build();
                updateProfileService = guestProfilesService.updateProfile().invoke(profile);
            }
            
            CompletionStage<NotUsed> updateOptinsService = CompletableFuture.completedFuture(NotUsed.getInstance());
            Optins optins = Mapper.mapEnrichedGuestToOptins(enrichedGuest);
            
            if (optins != null && enrichedGuest.getSignInInformation() != null
                    && StringUtils.isNotBlank(enrichedGuest.getEmail())) {
                updateOptinsService = guestProfileOptinService
                        .updateOptins(enrichedGuest.getEmail()).invoke(optins);
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
                            if (accountFuture.isCompletedExceptionally()) {
                                sb.append("Update Account service failed. ");
                            }
                            
                            if (profileFuture.isCompletedExceptionally()) {
                                sb.append("Update Profile service failed. ");
                            }
                            
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
     * Update Account service processing for Saviynt which is not exposed as service endpoint.
     *
     * @return {@link HeaderServiceCall} with {@link Pair}
     */
    private ServiceCall<Guest, NotUsed> updateAccount() {
        return (guest) -> {
            
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
            
            // TODO remove these once the migration scenarios are finalized.
            ObjectNode mockResponse = OBJECT_MAPPER.createObjectNode();
            if ("legacyuser@rccl.com".equalsIgnoreCase(request.getUsername())) {
                mockResponse.put("accountLoginStatus", LoginStatusEnum.LEGACY_ACCOUNT_VERIFIED.value());
                return CompletableFuture.completedFuture(Pair.create(ResponseHeader.OK, mockResponse));
            } else if ("temporarypassword@rccl.com".equalsIgnoreCase(request.getUsername())) {
                mockResponse.put("accountLoginStatus", LoginStatusEnum.NEW_ACCOUNT_TEMPORARY_PASSWORD.value());
                return CompletableFuture.completedFuture(Pair.create(ResponseHeader.OK, mockResponse));
            }
            
            return forgeRockService.authenticateMobileUser()
                    .invoke(forgeRockCredentials)
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof ForgeRockExceptionFactory.AuthenticationException) {
                            ForgeRockExceptionFactory.AuthenticationException ex =
                                    (ForgeRockExceptionFactory.AuthenticationException) cause;
                            throw new GuestAuthenticationException(ex.getErrorDescription());
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(jsonNode -> {
                        ObjectNode jsonResponse = OBJECT_MAPPER.createObjectNode();
                        jsonResponse.put("accountLoginStatus", LoginStatusEnum.NEW_ACCOUNT_AUTHENTICATED.value());
                        jsonResponse.put("accessToken", jsonNode.get("access_token").asText());
                        jsonResponse.put("refreshToken", jsonNode.get("refresh_token").asText());
                        jsonResponse.put("openIdToken", jsonNode.get("id_token").asText());
                        jsonResponse.put("tokenExpiration", jsonNode.get("expires_in").asText());
                        
                        return Pair.create(ResponseHeader.OK.withStatus(200), jsonResponse);
                    });
            
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, JsonNode> validateEmail(String email) {
        final String STATUS = "status";
        return (requestHeader, notUsed) -> {
            
            Pattern pattern = Pattern.compile(ValidatorConstants.EMAIL_REGEXP);
            Matcher matcher = pattern.matcher(email);
            
            if (!matcher.matches()) {
                throw new InvalidEmailFormatException();
            }
            
            return saviyntService.getGuestAccount("email", Optional.of(email), Optional.empty())
                    .invoke()
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof SaviyntExceptionFactory.ExistingGuestException
                                || cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                            ObjectNode errorJson = OBJECT_MAPPER.createObjectNode();
                            errorJson.put(STATUS, AccountStatusEnum.DOESTNOTEXIST.value());
                            return errorJson;
                        }
                        
                        if (cause instanceof SaviyntExceptionFactory.InvalidEmailFormatException) {
                            throw new InvalidEmailFormatException();
                        }
                        
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(response -> {
                        ObjectNode jsonResponse = OBJECT_MAPPER.createObjectNode();
                        
                        //return response from exceptionally block if present.
                        if (response.get(STATUS) != null) {
                            jsonResponse = response.deepCopy();
                        } else {
                            if (response.get("SavCode") != null
                                    && response.get("SavCode").asText().contains("Sav000")) {
                                jsonResponse.put(STATUS, AccountStatusEnum.EXISTING.value());
                            } else {
                                jsonResponse.put(STATUS, AccountStatusEnum.NEEDSTOBEMIGRATED.value());
                            }
                        }
                        
                        ResponseHeader responseHeader = ResponseHeader.OK.withStatus(200);
                        return new Pair<>(responseHeader, jsonResponse);
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
