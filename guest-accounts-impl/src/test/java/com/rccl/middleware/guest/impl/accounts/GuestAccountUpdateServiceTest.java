package com.rccl.middleware.guest.impl.accounts;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.validation.MiddlewareValidationException;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.enriched.ContactInformation;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.enriched.SignInInformation;
import com.rccl.middleware.guest.accounts.enriched.TravelDocumentInformation;
import com.rccl.middleware.guest.accounts.enriched.WebshopperInformation;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.guestprofiles.GuestProfileServiceStub;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.guestprofiles.models.Address;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GuestAccountUpdateServiceTest {
    
    private static volatile ServiceTest.TestServer testServer;
    
    private static GuestAccountService guestAccountService;
    
    @BeforeClass
    public static void setUp() {
        final ServiceTest.Setup setup = defaultSetup()
                .configureBuilder(builder -> builder.overrides(
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class),
                        bind(GuestProfilesService.class).to(GuestProfileServiceStub.class),
                        bind(GuestAccountService.class).to(GuestAccountServiceImpl.class)
                ));
        
        testServer = startServer(setup.withCassandra(true));
        guestAccountService = testServer.client(GuestAccountService.class);
    }
    
    @AfterClass
    public static void tearDown() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
        }
    }
    
    @Test
    public void shouldUpdateGuestSuccessfully() {
        EnrichedGuest guest = this.createSampleEnrichedGuest().build();
        
        try {
            HeaderServiceCall<EnrichedGuest, JsonNode> updateAccount =
                    (HeaderServiceCall<EnrichedGuest, JsonNode>) guestAccountService.updateAccountEnriched();
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue("Response must not be null.", response != null);
            assertTrue("Response Header Status must be 200", response.first().status() == 200);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of MiddlewareTransportException",
                    e.getCause() instanceof MiddlewareTransportException);
        }
    }
    
    @Test
    public void shouldFailGuestUpdate() {
        EnrichedGuest guest = this.createSampleEnrichedGuest().vdsId("G765432").build();
        
        try {
            HeaderServiceCall<EnrichedGuest, JsonNode> updateAccount =
                    (HeaderServiceCall<EnrichedGuest, JsonNode>) guestAccountService.updateAccountEnriched();
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue(response != null);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of NoSuchGuestException.",
                    e instanceof SaviyntExceptionFactory.NoSuchGuestException);
        }
    }
    
    @Test(expected = MiddlewareValidationException.class)
    public void shouldFailUpdateWithInvalidFields() throws Exception {
        EnrichedGuest guest = this.createSampleEnrichedGuest()
                .signInInformation(SignInInformation.builder()
                        .password("123".toCharArray())
                        .build())
                .email("invalidemail")
                .build();
        
        HeaderServiceCall<EnrichedGuest, JsonNode> updateAccount =
                (HeaderServiceCall<EnrichedGuest, JsonNode>) guestAccountService.updateAccountEnriched();
        
        Pair<ResponseHeader, JsonNode> response = updateAccount
                .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        
        assertTrue(response != null);
    }
    
    private EnrichedGuest.EnrichedGuestBuilder createSampleEnrichedGuest() {
        return EnrichedGuest.builder()
                .header(Header.builder().brand('R').channel("app-ios").build())
                .contactInformation(ContactInformation.builder()
                        .address(Address.builder()
                                .addressOne("Address one")
                                .city("City")
                                .state("FL")
                                .zipCode("12345")
                                .build())
                        .phoneNumber("123-456-7890")
                        .phoneCountryCode("+1")
                        .build())
                .emergencyContact(EmergencyContact.builder()
                        .phoneNumber("123-456-7890")
                        .firstName("First")
                        .lastName("Last")
                        .relationship("Mother")
                        .build())
                .signInInformation(SignInInformation.builder()
                        .password("password1".toCharArray())
                        .securityQuestions(
                                Arrays.asList(SecurityQuestion.builder().question("what?").answer("yes").build())
                        )
                        .build())
                .travelDocumentInformation(TravelDocumentInformation.builder()
                        .passportNumber("1234567890")
                        .passportExpirationDate("20200101")
                        .birthCountryCode("USA")
                        .citizenshipCountryCode("USA")
                        .build())
                .webshopperInformation(WebshopperInformation.builder().brand('R').shopperId("123456789").build())
                .email("successful@domain.com")
                .vdsId("G1234567");
    }
}
