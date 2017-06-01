package com.rccl.middleware.guest.impl.accounts;

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
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.exceptions.ExistingGuestException;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.guest.pingfederate.PingFederateService;
import com.rccl.middleware.guest.pingfederate.PingFederateSubject;
import com.rccl.middleware.guest.saviynt.SaviyntGuest;
import com.rccl.middleware.guest.saviynt.SaviyntService;
import com.rccl.middleware.guest.saviynt.exceptions.SaviyntExceptionFactory;
import play.Configuration;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GuestAccountServiceImpl implements GuestAccountService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private static final String UPDATE_GUEST_CONFIG_PATH = "update-account";
    
    private final GuestValidator guestValidator;
    
    private final PersistentEntityRegistry persistentEntityRegistry;
    
    private final PingFederateService pingFederateService;
    
    private final SaviyntService saviyntService;
    
    private final List<Link> updateAccountLinks;
    
    @Inject
    public GuestAccountServiceImpl(GuestValidator guestValidator,
                                   PingFederateService pingFederateService,
                                   SaviyntService saviyntService,
                                   Configuration configuration,
                                   PersistentEntityRegistry persistentEntityRegistry) {
        this.guestValidator = guestValidator;
        this.pingFederateService = pingFederateService;
        this.saviyntService = saviyntService;
        
        this.persistentEntityRegistry = persistentEntityRegistry;
        persistentEntityRegistry.register(GuestAccountEntity.class);
        
        HATEOASLinks hateoasLinks = new HATEOASLinks(configuration);
        updateAccountLinks = hateoasLinks.getLinks(UPDATE_GUEST_CONFIG_PATH);
    }
    
    @Override
    public HeaderServiceCall<Guest, String> createAccount() {
        return (requestHeader, guest) -> {
            guestValidator.validate(guest);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest).build();
            
            // Begin by invoking the Saviynt create account service.
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
                    // Upon success of Saviynt, invoke the PingFederate service to "log" the user in,
                    // and return the reference ID.
                    .thenCompose(saviyntResponse -> {
                        persistentEntityRegistry.refFor(GuestAccountEntity.class, guest.getEmail())
                                .ask(new GuestAccountCommand.CreateGuest(guest));
                        
                        PingFederateSubject pfs = PingFederateSubject.builder().subject(guest.getEmail()).build();
                        
                        return pingFederateService
                                .generateReferenceId()
                                .invoke(pfs)
                                .exceptionally(exception -> {
                                    // TODO: Handle ConnectException for invalid SSL certificates.
                                    throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), exception);
                                })
                                .thenApply(referenceId -> new Pair<>(ResponseHeader.OK.withStatus(201), referenceId.getValue()));
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
                    .termsAndConditionsAgreement(partialGuest.getTermsAndConditionsAgreement())
                    .build();
            
            guestValidator.validateGuestUpdateModel(guest);
            
            final SaviyntGuest saviyntGuest = mapGuestToSaviyntGuest(guest).email(email).build();
            
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
    
    private SaviyntGuest.SaviyntGuestBuilder mapGuestToSaviyntGuest(Guest guest) {
        SaviyntGuest.SaviyntGuestBuilder builder = SaviyntGuest.builder()
                .firstname(guest.getFirstName())
                .lastname(guest.getLastName())
                .displayname(guest.getFirstName() + " " + guest.getLastName())
                .username(guest.getEmail())
                .email(guest.getEmail())
                .password(guest.getPassword())
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
                .royalWebShopperIds(this.mapValuesToSaviyntStringFormat(guest.getRoyalWebShopperIds()));
        
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
