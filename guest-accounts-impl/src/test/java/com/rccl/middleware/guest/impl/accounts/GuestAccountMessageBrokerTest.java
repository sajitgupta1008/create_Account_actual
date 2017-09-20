package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.forgerock.api.ForgeRockService;
import com.rccl.middleware.forgerock.api.ForgeRockServiceImplStub;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.Optin;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GuestAccountMessageBrokerTest {
    
    private static ActorSystem system;
    
    private static volatile ServiceTest.TestServer testServer;
    
    private static GuestAccountService guestAccountService;
    
    private static PersistentEntityTestDriver<GuestAccountCommand, GuestAccountEvent, GuestAccountState> driver;
    
    @BeforeClass
    public static void setUp() {
        final ServiceTest.Setup setup = defaultSetup()
                .configureBuilder(builder -> builder.overrides(
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(ForgeRockService.class).to(ForgeRockServiceImplStub.class),
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class),
                        bind(GuestProfilesService.class).to(GuestProfileServiceStub.class)
                ));
        
        testServer = startServer(setup.withCassandra(true));
        guestAccountService = testServer.client(GuestAccountService.class);
        
        system = ActorSystem.create();
        driver = new PersistentEntityTestDriver<>(system, new GuestAccountEntity(), "guest-accounts");
    }
    
    @AfterClass
    public static void tearDown() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
        }
        
        system.terminate();
        system = null;
    }
    
    @Test
    public void testCreateGuestAccountPersistentEntity() {
        Guest sampleGuest = this.createSampleGuest();
        
        Outcome<GuestAccountEvent, GuestAccountState> outcome = driver.run(GuestAccountCommand.CreateGuest
                .builder()
                .guest(sampleGuest)
                .build());
        
        assertThat(outcome.events().get(0), is(instanceOf(GuestAccountEvent.GuestCreated.class)));
        assertThat(outcome.events().size(), is(equalTo(1)));
        assertThat(outcome.state().getGuest(), is(equalTo(sampleGuest)));
        assertThat(outcome.state().getEvent(), is(equalTo(GuestEventStatus.CREATE)));
        assertThat(outcome.getReplies().get(0), is(equalTo(Done.getInstance())));
        assertThat(outcome.issues().isEmpty(), is(true));
    }
    
    @Test
    public void testUpdateGuestAccountPersistentEntity() {
        EnrichedGuest sampleGuest = this.createSampleEnrichedGuest().build();
        
        Outcome<GuestAccountEvent, GuestAccountState> outcome = driver.run(GuestAccountCommand.UpdateGuest
                .builder()
                .enrichedGuest(sampleGuest)
                .build());
        
        assertThat(outcome.events().get(0), is(instanceOf(GuestAccountEvent.GuestUpdated.class)));
        assertThat(outcome.events().size(), is(equalTo(1)));
        assertThat(outcome.state().getEnrichedGuest(), is(equalTo(sampleGuest)));
        assertThat(outcome.state().getEvent(), is(equalTo(GuestEventStatus.UPDATE)));
        assertThat(outcome.getReplies().get(0), is(equalTo(Done.getInstance())));
        assertThat(outcome.issues().isEmpty(), is(true));
    }
    
    @Test
    public void testGuestAccountKafkaPublishing() {
        Source<GuestEvent, ?> linkLoyaltySource = guestAccountService.linkLoyaltyTopic().subscribe().atMostOnceSource();
        
        TestSubscriber.Probe<GuestEvent> linkLoyaltyProbe = linkLoyaltySource
                .runWith(
                        TestSink.probe(testServer.system()), testServer.materializer()
                );
        
        Source<EnrichedGuest, ?> verifyLoyaltySource = guestAccountService.verifyLoyaltyTopic()
                .subscribe().atMostOnceSource();
        
        TestSubscriber.Probe<EnrichedGuest> verifyLoyaltyProbe = verifyLoyaltySource
                .runWith(
                        TestSink.probe(testServer.system()), testServer.materializer()
                );
        
        
        Guest sampleGuest = this.createSampleGuest();
        EnrichedGuest sampleEnrichedGuest = this.createSampleEnrichedGuest().build();
        
        try {
            FiniteDuration finiteDuration = new FiniteDuration(20, SECONDS);
            
            guestAccountService.createAccount()
                    .invoke(sampleGuest)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            
            GuestEvent createGuestEvent = linkLoyaltyProbe.request(1).expectNext(finiteDuration);
            assertTrue(createGuestEvent instanceof GuestEvent.AccountCreated);
            
            guestAccountService.updateAccountEnriched()
                    .invoke(sampleEnrichedGuest)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            
            GuestEvent updateGuestEvent = linkLoyaltyProbe.request(1).expectNext(finiteDuration);
            assertTrue(updateGuestEvent instanceof GuestEvent.AccountUpdated);
            
            EnrichedGuest verifyLoyaltyEvent = verifyLoyaltyProbe.request(1).expectNext(finiteDuration);
            assertTrue(verifyLoyaltyEvent != null);
            
        } catch (Exception e) {
            assertTrue("The service thrown an exception.", false);
        }
    }
    
    private Guest createSampleGuest() {
        return Guest.builder()
                .header(Header.builder()
                        .channel("web")
                        .brand('R')
                        .locale(Locale.US)
                        .build())
                .email("successful@domain.com")
                .firstName("John")
                .lastName("Downs")
                .consumerId("123456789")
                .password("password1".toCharArray())
                .birthdate("19910101")
                .phoneNumber("+1(234)456-7890")
                .securityQuestions(Collections.singletonList(SecurityQuestion.builder().question("what?").answer("yes").build()))
                .termsAndConditionsAgreement(TermsAndConditionsAgreement.builder()
                        .acceptTime("20170627033735PM")
                        .version("1.0")
                        .build())
                .optins(Collections.singletonList(Optin.builder().type("EMAIL").flag("Y").acceptTime("20170706022122PM").build()))
                .build();
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
                                Collections.singletonList(SecurityQuestion.builder().question("what?").answer("yes").build())
                        )
                        .build())
                .travelDocumentInformation(TravelDocumentInformation.builder()
                        .passportNumber("1234567890")
                        .passportExpirationDate("20200101")
                        .birthCountryCode("USA")
                        .citizenshipCountryCode("USA")
                        .build())
                .webshopperInformation(WebshopperInformation.builder().brand('R').shopperId("123456789").build())
                .consumerId("1234567")
                .email("successful@domain.com")
                .vdsId("G1234567");
    }
}
