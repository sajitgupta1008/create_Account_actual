package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.hateoas.HATEOASLinks;
import com.rccl.middleware.common.hateoas.Link;
import com.rccl.middleware.common.validation.MiddlewareValidation;
import com.rccl.middleware.forgerock.api.ForgeRockCredentials;
import com.rccl.middleware.forgerock.api.ForgeRockService;
import com.rccl.middleware.guest.accounts.AccountStatusEnum;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.exceptions.ExistingGuestException;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidEmailFormatException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guest.optin.OptinType;
import com.rccl.middleware.guest.optin.Optins;
import com.rccl.middleware.saviynt.api.SaviyntGuest;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntUserType;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import play.Configuration;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String UPDATE_GUEST_CONFIG_PATH = "update-account";
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final SaviyntService saviyntService;
    
    private final ForgeRockService forgeRockService;
    
    private final GuestProfileOptinService guestProfileOptinService;
    
    private final List<Link> updateAccountLinks;
    
    @Inject
    public GuestAccountServiceImpl(SaviyntService saviyntService,
                                   ForgeRockService forgeRockService,
                                   Configuration configuration,
                                   PersistentEntityRegistry persistentEntityRegistry,
                                   GuestProfileOptinService guestProfileOptinService) {
        this.saviyntService = saviyntService;
        this.forgeRockService = forgeRockService;
        
        this.guestProfileOptinService = guestProfileOptinService;
        
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(GuestAccountEntity.class);
        
        HATEOASLinks hateoasLinks = new HATEOASLinks(configuration);
        updateAccountLinks = hateoasLinks.getLinks(UPDATE_GUEST_CONFIG_PATH);
    }
    
    @Override
    public HeaderServiceCall<Guest, JsonNode> createAccount() {
        return (requestHeader, guest) -> {
            MiddlewareValidation.validateWithGroups(guest, Guest.CreateChecks.class);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest, true).build();
            
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
                        
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(guest));
                        
                        // trigger optin service to store the optins into Cassandra
                        guestProfileOptinService.createOptins(guest.getEmail())
                                .invoke(this.generateCreateOptinsRequest(guest))
                                .toCompletableFuture().complete(NotUsed.getInstance());
                        
                        //TODO replace this with the vdsId attribute when available
                        String message = response.get("message").asText();
                        Pattern pattern = Pattern.compile("vdsid=[a-zA-Z0-9]*");
                        Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        String vdsId = matcher.group(0).substring(6);
                        
                        return this.authenticateUser(guest, vdsId);
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<Guest, JsonNode> updateAccount(String email) {
        return (requestHeader, partialGuest) -> {
            
            final Guest guest = Guest.builder()
                    .header(partialGuest.getHeader())
                    .email(email)
                    .firstName(partialGuest.getFirstName())
                    .lastName(partialGuest.getLastName())
                    .birthdate(partialGuest.getBirthdate())
                    .securityQuestions(partialGuest.getSecurityQuestions())
                    .consumerId(partialGuest.getConsumerId())
                    .crownAndAnchorIds(partialGuest.getCrownAndAnchorIds())
                    .captainsClubIds(partialGuest.getCaptainsClubIds())
                    .azamaraLoyaltyIds(partialGuest.getAzamaraLoyaltyIds())
                    .clubRoyaleIds(partialGuest.getClubRoyaleIds())
                    .celebrityBlueChipIds(partialGuest.getCelebrityBlueChipIds())
                    .azamaraBookingIds(partialGuest.getAzamaraBookingIds())
                    .celebrityBookingIds(partialGuest.getCelebrityBookingIds())
                    .royalBookingIds(partialGuest.getRoyalBookingIds())
                    .azamaraWebShopperIds(partialGuest.getAzamaraWebShopperIds())
                    .celebrityWebShopperIds(partialGuest.getCelebrityWebShopperIds())
                    .royalWebShopperIds(partialGuest.getRoyalWebShopperIds())
                    .royalPrimaryBookingId(partialGuest.getRoyalPrimaryBookingId())
                    .celebrityPrimaryBookingId(partialGuest.getCelebrityPrimaryBookingId())
                    .azamaraPrimaryBookingId(partialGuest.getAzamaraPrimaryBookingId())
                    .termsAndConditionsAgreement(partialGuest.getTermsAndConditionsAgreement())
                    .build();
            
            MiddlewareValidation.validateWithGroups(guest, Guest.UpdateChecks.class);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest, false).email(email).build();
            
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
                    .thenApply(response -> {
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, email)
                                .ask(new GuestAccountCommand.UpdateGuest(guest));
                        
                        final ObjectNode objNode = OBJECT_MAPPER.createObjectNode();
                        updateAccountLinks.forEach(link -> link.substituteArguments(email));
                        objNode.putPOJO("_links", updateAccountLinks);
                        ResponseHeader responseHeader = ResponseHeader.OK.withStatus(200);
                        
                        return new Pair<>(responseHeader, objNode);
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<NotUsed, JsonNode> validateEmail(String email) {
        return (requestHeader, notUsed) -> {
            
            String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+((\\.[A-Za-z0-9]+)){1,2}$";
            Pattern pattern = Pattern.compile(emailPattern);
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
                            errorJson.put("status", AccountStatusEnum.DOESTNOTEXIST.value());
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
                        if (response.get("status") != null) {
                            jsonResponse = response.deepCopy();
                        } else {
                            if (response.get("SavCode") != null && response.get("SavCode").asText().contains("Sav000")) {
                                jsonResponse.put("status", AccountStatusEnum.EXISTING.value());
                            } else {
                                jsonResponse.put("status", AccountStatusEnum.NEEDSTOBEMIGRATED.value());
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
    
    @Override
    public Topic<GuestEvent> guestAccountsTopic() {
        return TopicProducer.taggedStreamWithOffset(GuestAccountTag.GUEST_ACCOUNT_EVENT_TAG.allTags(), (tag, offset) ->
                persistentEntityRegistry.eventStream(tag, offset)
                        .mapAsync(1, eventOffset -> {
                            GuestAccountEvent event = eventOffset.first();
                            GuestEvent guestEvent;
                            if (event instanceof GuestAccountEvent.GuestCreated) {
                                GuestAccountEvent.GuestCreated eventInstance = (GuestAccountEvent.GuestCreated) event;
                                guestEvent = new GuestEvent.AccountCreated(eventInstance.getGuest());
                                
                            } else {
                                GuestAccountEvent.GuestUpdated eventInstance = (GuestAccountEvent.GuestUpdated) event;
                                guestEvent = new GuestEvent.AccountUpdated(eventInstance.getGuest());
                            }
                            
                            return CompletableFuture.completedFuture(new Pair<>(guestEvent, eventOffset.second()));
                        }));
    }
    
    /**
     * Creates a builder which maps the appropriate {@link Guest} values into {@link SaviyntGuest} object based on the action taken.
     *
     * @param guest    the {@link Guest} model
     * @param isCreate determines the request being taken whether it is create or update guest account
     * @return {@link SaviyntGuest.SaviyntGuestBuilder}
     */
    private SaviyntGuest.SaviyntGuestBuilder mapGuestToSaviyntGuest(Guest guest, boolean isCreate) {
        SaviyntGuest.SaviyntGuestBuilder builder = SaviyntGuest.builder()
                .firstname(guest.getFirstName())
                .lastname(guest.getLastName())
                .displayname(guest.getFirstName() + " " + guest.getLastName())
                .email(guest.getEmail())
                .password(guest.getPassword())
                .dateofBirth(guest.getBirthdate())
                .consumerId(guest.getConsumerId())
                .crownAndAnchorIds(this.mapValuesToSaviyntStringFormat(guest.getCrownAndAnchorIds()))
                .captainsClubIds(this.mapValuesToSaviyntStringFormat(guest.getCaptainsClubIds()))
                .azamaraLoyaltyIds(this.mapValuesToSaviyntStringFormat(guest.getAzamaraLoyaltyIds()))
                .clubRoyaleIds(this.mapValuesToSaviyntStringFormat(guest.getClubRoyaleIds()))
                .celebrityBlueChipIds(this.mapValuesToSaviyntStringFormat(guest.getCelebrityBlueChipIds()))
                .azamaraBookingIds(this.mapValuesToSaviyntStringFormat(guest.getAzamaraBookingIds()))
                .celebrityBookingIds(this.mapValuesToSaviyntStringFormat(guest.getCelebrityBookingIds()))
                .royalBookingIds(this.mapValuesToSaviyntStringFormat(guest.getRoyalBookingIds()))
                .azamaraWebShopperIds(this.mapValuesToSaviyntStringFormat(guest.getAzamaraWebShopperIds()))
                .celebrityWebShopperIds(this.mapValuesToSaviyntStringFormat(guest.getCelebrityWebShopperIds()))
                .royalWebShopperIds(this.mapValuesToSaviyntStringFormat(guest.getRoyalWebShopperIds()))
                .royalPrimaryBookingId(guest.getRoyalPrimaryBookingId())
                .celebrityPrimaryBookingId(guest.getCelebrityPrimaryBookingId())
                .azamaraPrimaryBookingId(guest.getAzamaraPrimaryBookingId())
                .propertytosearch("email");
        
        // only map the account creation specific attributes
        if (isCreate) {
            builder.username(guest.getEmail())
                    .userType(SaviyntUserType.Guest);
        }
        
        List<SecurityQuestion> securityQuestions = guest.getSecurityQuestions();
        
        if (securityQuestions != null && !securityQuestions.isEmpty()) {
            SecurityQuestion sq = securityQuestions.get(0);
            
            builder
                    .securityquestion(sq.getQuestion())
                    .securityanswer(sq.getAnswer());
        }
        
        if (guest.getTermsAndConditionsAgreement() != null) {
            builder.termsAndConditionsVersion(guest.getTermsAndConditionsAgreement().getVersion());
        }
        
        return builder;
    }
    
    /**
     * Triggers ForgeRock authentication for either mobile or web depending on the channel provided from the header.
     *
     * @param guest the {@link Guest} model.
     * @param vdsId the vdsId from Saviynt's create account response.
     * @return {@link CompletionStage}
     */
    private CompletionStage<Pair<ResponseHeader, JsonNode>> authenticateUser(Guest guest, String vdsId) {
        ForgeRockCredentials forgeRockCredentials = ForgeRockCredentials.builder()
                .username(guest.getEmail())
                .password(guest.getPassword())
                .build();
        
        // TODO get the proper value for channel
        if ("web".equals(guest.getHeader().getChannel())) {
            return forgeRockService.authenticateWebUser()
                    .invoke(forgeRockCredentials)
                    .exceptionally(exception -> {
                        // how to handle exception here if something happens?
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(jsonNode -> {
                        ObjectNode jsonResponse = OBJECT_MAPPER.createObjectNode();
                        jsonResponse.put("vdsId", vdsId);
                        jsonResponse.put("tokenId", jsonNode.get("tokenId").asText());
                        
                        return Pair.create(ResponseHeader.OK.withStatus(201), jsonResponse);
                    });
        } else {
            return forgeRockService.authenticateMobileUser()
                    .invoke(forgeRockCredentials)
                    .exceptionally(exception -> {
                        // how to handle exception here if something happens?
                        throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                    })
                    .thenApply(jsonNode -> {
                        ObjectNode jsonResponse = OBJECT_MAPPER.createObjectNode();
                        jsonResponse.put("vdsId", vdsId);
                        jsonResponse.put("accessToken", jsonNode.get("access_token").asText());
                        jsonResponse.put("refreshToken", jsonNode.get("refresh_token").asText());
                        jsonResponse.put("tokenId", jsonNode.get("id_token").asText());
                        jsonResponse.put("tokenExpiration", jsonNode.get("expires_in").asText());
                        
                        return Pair.create(ResponseHeader.OK.withStatus(201), jsonResponse);
                    });
        }
        
        
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
                        .flag(true)
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
                .email(guest.getEmail())
                .optins(optinList)
                .build();
    }
    
    /**
     * Wraps each {@link List} value with quotation marks to satisfy Saviynt's requirement.
     *
     * @param attributeList {@code List<String>}
     * @return {@code List<String>}
     */
    private List<String> mapValuesToSaviyntStringFormat(List<String> attributeList) {
        if (attributeList != null && !attributeList.isEmpty()) {
            return attributeList
                    .stream()
                    .map(val -> "\"" + val + "\"")
                    .collect(Collectors.toList());
        }
        
        return null;
    }
}
