package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.hateoas.HATEOASLinks;
import com.rccl.middleware.common.hateoas.Link;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.exceptions.ExistingGuestException;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.saviynt.api.SaviyntGuest;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntUserType;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import play.Configuration;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String UPDATE_GUEST_CONFIG_PATH = "update-account";
    
    private final GuestValidator guestValidator;
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final SaviyntService saviyntService;
    
    private final List<Link> updateAccountLinks;
    
    private final String CREATE_ACCOUNT = "create";
    
    private final String UPDATE_ACCOUNT = "update";
    
    @Inject
    public GuestAccountServiceImpl(GuestValidator guestValidator,
                                   SaviyntService saviyntService,
                                   Configuration configuration,
                                   PersistentEntityRegistry persistentEntityRegistry) {
        this.guestValidator = guestValidator;
        this.saviyntService = saviyntService;
        
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(GuestAccountEntity.class);
        
        HATEOASLinks hateoasLinks = new HATEOASLinks(configuration);
        updateAccountLinks = hateoasLinks.getLinks(UPDATE_GUEST_CONFIG_PATH);
    }
    
    @Override
    public HeaderServiceCall<Guest, TextNode> createAccount() {
        return (requestHeader, guest) -> {
            guestValidator.validate(guest);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest, CREATE_ACCOUNT).build();
            
            return saviyntService
                    .postGuestAccount()
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
                    .thenApply(response -> {
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(guest));
                        
                        //TODO replace this with the vdsId attribute when available
                        String message = response.get("message").asText();
                        Pattern pattern = Pattern.compile("vdsid=[a-zA-Z0-9]*");
                        Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        String vdsId = matcher.group(0).substring(6);
                        
                        return new Pair<>(ResponseHeader.OK.withStatus(201), TextNode.valueOf(vdsId));
                    });
        };
    }
    
    @Override
    public HeaderServiceCall<Guest, JsonNode> updateAccount(String email) {
        return (requestHeader, partialGuest) -> {
            
            final Guest guest = Guest.builder()
                    .email(email)
                    .firstName(partialGuest.getFirstName())
                    .lastName(partialGuest.getLastName())
                    .dateOfBirth(partialGuest.getDateOfBirth())
                    .securityQuestions(partialGuest.getSecurityQuestions())
                    .brand(partialGuest.getBrand())
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
            
            guestValidator.validateGuestUpdateModel(guest);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest, UPDATE_ACCOUNT).email(email).build();
            
            return saviyntService
                    .putGuestAccount()
                    .invoke(saviyntGuest)
                    .exceptionally(exception -> {
                        Throwable cause = exception.getCause();
                        
                        if (cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
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
     * @param guest the {@link Guest} model
     * @param action the request being taken whether it is create or update guest account
     * @return {@link SaviyntGuest.SaviyntGuestBuilder}
     */
    private SaviyntGuest.SaviyntGuestBuilder mapGuestToSaviyntGuest(Guest guest, String action) {
        SaviyntGuest.SaviyntGuestBuilder builder = SaviyntGuest.builder()
                .firstname(guest.getFirstName())
                .lastname(guest.getLastName())
                .displayname(guest.getFirstName() + " " + guest.getLastName())
                .email(guest.getEmail())
                .password(guest.getPassword())
                .dateofBirth(guest.getDateOfBirth())
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
                .azamaraPrimaryBookingId(guest.getAzamaraPrimaryBookingId());
        
        // only map the account creation specific attributes
        if (action.equals(CREATE_ACCOUNT)) {
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
