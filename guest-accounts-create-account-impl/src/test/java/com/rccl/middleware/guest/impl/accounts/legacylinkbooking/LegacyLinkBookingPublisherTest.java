package com.rccl.middleware.guest.impl.accounts.legacylinkbooking;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.email.AemEmailServiceStub;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.request.EnvironmentDetails;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.PrivacyPolicyAgreement;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.accounts.legacylinkbooking.LegacyLinkBookingMessage;
import com.rccl.middleware.guest.authentication.GuestAuthenticationService;
import com.rccl.middleware.guest.authentication.GuestAuthenticationServiceStub;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.guestprofiles.GuestProfileServiceStub;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import com.rccl.middleware.vds.VDSService;
import com.rccl.middleware.vds.VDSServiceStub;
import com.rccl.middleware.vds.responses.WebShopperView;
import com.rccl.middleware.vds.responses.WebShopperViewList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class LegacyLinkBookingPublisherTest {
    
    private static final String CONSUMER_ID_ONE = "1";
    private static final String CONSUMER_ID_TWO = "2";
    private static final String EXPECTED_WEBSHOPPER_USERNAME = "webshopper@email.com";
    private static final String RESERVATION_ID_ONE = "webShopperUserId1";
    private static final String RESERVATION_ID_TWO = "webShopperUserId2";
    private static final String WEBSHOPPER_ID_ONE = "7654321";
    private static final String WEBSHOPPER_ID_TWO = "7654322";
    
    private static ActorSystem actorSystem;
    
    private static volatile ServiceTest.TestServer testServer;
    
    private static GuestAccountService service;
    
    private static PersistentEntityTestDriver<LegacyLinkBookingCommand,
            LegacyLinkBookingEvent,
            LegacyLinkBookingState> driver;
    
    @BeforeClass
    public static void beforeClass() {
        final ServiceTest.Setup setup = defaultSetup()
                .configureBuilder(builder -> builder.overrides(
                        bind(AemEmailService.class).to(AemEmailServiceStub.class),
                        bind(GuestAuthenticationService.class).to(GuestAuthenticationServiceStub.class),
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class),
                        bind(GuestProfilesService.class).to(GuestProfileServiceStub.class),
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(VDSService.class).to(CustomVDSService.class)
                ));
        
        testServer = startServer(setup.withCassandra(true));
        service = testServer.client(GuestAccountService.class);
        
        actorSystem = ActorSystem.create();
        driver = new PersistentEntityTestDriver<>(actorSystem, new LegacyLinkBookingEntity(), "legacy-link-account");
    }
    
    @AfterClass
    public static void afterClass() {
        if (testServer != null) {
            testServer.stop();
        }
        
        if (actorSystem != null) {
            actorSystem.terminate();
        }
    }
    
    @Test
    public void testPersistentEntity() {
        LegacyLinkBookingMessage expectedMessage = LegacyLinkBookingMessage
                .builder()
                .build();
        
        PersistentEntityTestDriver
                .Outcome<LegacyLinkBookingEvent, LegacyLinkBookingState> outcome = driver.run(new LegacyLinkBookingCommand(expectedMessage));
        assertNotNull(outcome);
        
        List<LegacyLinkBookingEvent> events = outcome.events();
        assertNotNull(events);
        
        int expectedNumberOfEvents = 1;
        int actualNumberOfEvents = events.size();
        assertEquals(expectedNumberOfEvents, actualNumberOfEvents);
        
        LegacyLinkBookingEvent event = events.get(0);
        assertTrue(LegacyLinkBookingEvent.class.isAssignableFrom(event.getClass()));
        
        LegacyLinkBookingState state = outcome.state();
        assertNotNull(state);
        
        LegacyLinkBookingMessage message = state.getMessage();
        assertNotNull(message);
        
        List<Object> replies = outcome.getReplies();
        assertEquals(replies.get(0), Done.getInstance());
    }
    
    @Test
    public void testPublish() {
        Source<LegacyLinkBookingMessage, ?> source = service.legacyLinkBookingTopic()
                .subscribe()
                .atMostOnceSource();
        
        TestSubscriber.Probe<LegacyLinkBookingMessage> probe = source
                .runWith(TestSink.probe(testServer.system()), testServer.materializer());
        
        Guest request = createSampleGuest();
        
        service.createAccount()
                .handleRequestHeader(rh -> rh
                        .withHeader(EnvironmentDetails.ENVIRONMENT_MARKER_HEADER_NAME, "shore")
                        .withHeader(EnvironmentDetails.ENVIRONMENT_SHIP_CODE_HEADER_NAME, "none"))
                .invoke(request)
                .toCompletableFuture()
                .join();
        
        LegacyLinkBookingMessage event = probe.requestNext();
        
        assertNotNull(event);
        
        assertEquals(request.getHeader().getBrand().toString(), event.getBrand());
        assertEquals(Arrays.asList(CONSUMER_ID_ONE, CONSUMER_ID_TWO), event.getConsumerIds());
        assertEquals(request, event.getGuest());
        assertEquals(Arrays.asList(RESERVATION_ID_ONE, RESERVATION_ID_TWO), event.getReservationUserIds());
        assertEquals(Arrays.asList(WEBSHOPPER_ID_ONE, WEBSHOPPER_ID_TWO), event.getWebshopperIds());
    }
    
    static final class CustomVDSService extends VDSServiceStub {
        
        @Override
        public ServiceCall<NotUsed, WebShopperViewList> getWebShopperAttributes(String filter) {
            return notUsed -> {
                WebShopperView resultOne = WebShopperView.builder()
                        .consumerId(CONSUMER_ID_ONE)
                        .email("webshopper@email.com")
                        .reservationUserId(RESERVATION_ID_ONE)
                        .webshopperId(WEBSHOPPER_ID_ONE)
                        .webshopperUsername(EXPECTED_WEBSHOPPER_USERNAME)
                        .build();
                
                WebShopperView resultTwo = WebShopperView.builder()
                        .consumerId(CONSUMER_ID_TWO)
                        .email("webshopper2@email.com")
                        .reservationUserId(RESERVATION_ID_TWO)
                        .webshopperId(WEBSHOPPER_ID_TWO)
                        .webshopperUsername(EXPECTED_WEBSHOPPER_USERNAME)
                        .build();
                
                WebShopperViewList webShopperViewList = WebShopperViewList.builder()
                        .webshopperViews(Arrays.asList(resultOne, resultTwo))
                        .build();
                
                return CompletableFuture.completedFuture(webShopperViewList);
            };
        }
    }
    
    private Guest createSampleGuest() {
        return Guest.builder()
                .header(Header.builder()
                        .channel("web")
                        .brand('R')
                        .locale(Locale.US)
                        .build())
                .email("webshopper@email.com")
                .firstName("John")
                .lastName("Downs")
                .consumerId("123456789")
                .password("password1".toCharArray())
                .birthdate("19910101")
                .phoneNumber("+1(234)456-7890")
                .webshopperId("7654321")
                .securityQuestions(Collections.singletonList(SecurityQuestion.builder()
                        .question("what?").answer("yes").build()))
                .termsAndConditionsAgreement(TermsAndConditionsAgreement.builder()
                        .acceptTime("20170627T033735UTC")
                        .version("1.0")
                        .build())
                .privacyPolicyAgreement(PrivacyPolicyAgreement.builder()
                        .acceptTime("20170627T033735UTC")
                        .version("1.0")
                        .build())
                .build();
    }
}
