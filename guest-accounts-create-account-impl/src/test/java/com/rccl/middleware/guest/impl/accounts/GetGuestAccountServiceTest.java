package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.guestprofiles.GuestProfileServiceStub;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GetGuestAccountServiceTest {
    
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
    public void testSuccessfulGetGuestAccountEnriched() {
        
        ResponseBody<EnrichedGuest> response = ((HeaderServiceCall<NotUsed, ResponseBody<EnrichedGuest>>) guestAccountService
                .getAccountEnriched("G1234567", Optional.of("true")))
                .invokeWithHeaders(RequestHeader.DEFAULT, NotUsed.getInstance()).toCompletableFuture().join().second();
        
        EnrichedGuest guest = response.getPayload();
        assertTrue(guest != null);
        assertTrue(guest.getPersonalInformation().getAvatar() != null);
        assertTrue(guest.getContactInformation().getPhoneNumber() != null);
        assertTrue(guest.getEmergencyContact().getLastName() != null);
        assertTrue(guest.getLoyaltyInformation().getCrownAndAnchorSocietyLoyaltyTier() != null);
        assertTrue(guest.getTravelDocumentInformation().getBirthCountryCode() != null);
        assertTrue(guest.getConsumerId() != null);
    }
    
    @Test(expected = SaviyntExceptionFactory.NoSuchGuestException.class)
    public void testNonExistingGuest() {
        ((HeaderServiceCall<NotUsed, ResponseBody<EnrichedGuest>>) guestAccountService
                .getAccountEnriched("G1111111", Optional.of("true")))
                .invokeWithHeaders(RequestHeader.DEFAULT, NotUsed.getInstance()).toCompletableFuture().join().second();
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testEmptyVdsIdValidation() {
        guestAccountService.getAccountEnriched("", Optional.of("true")).invoke().toCompletableFuture().join();
    }
}
