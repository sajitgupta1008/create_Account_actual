package com.rccl.middleware.guest.impl.accounts;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.JavaTestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.GuestEvent;
import com.rccl.middleware.guest.accounts.Optin;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
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
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class)
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
        
        JavaTestKit.shutdownActorSystem(system);
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
        Guest sampleGuest = this.createSampleGuest();
        
        Outcome<GuestAccountEvent, GuestAccountState> outcome = driver.run(GuestAccountCommand.UpdateGuest
                .builder()
                .guest(sampleGuest)
                .build());
        
        assertThat(outcome.events().get(0), is(instanceOf(GuestAccountEvent.GuestUpdated.class)));
        assertThat(outcome.events().size(), is(equalTo(1)));
        assertThat(outcome.state().getGuest(), is(equalTo(sampleGuest)));
        assertThat(outcome.state().getEvent(), is(equalTo(GuestEventStatus.UPDATE)));
        assertThat(outcome.getReplies().get(0), is(equalTo(Done.getInstance())));
        assertThat(outcome.issues().isEmpty(), is(true));
    }
    
    @Test
    public void testGuestAccountKafkaPublishing() {
        Source<GuestEvent, ?> source = guestAccountService.guestAccountsTopic().subscribe().atMostOnceSource();
        
        TestSubscriber.Probe<GuestEvent> probe = source
                .runWith(
                        TestSink.probe(testServer.system()), testServer.materializer()
                );
        
        Guest sampleGuest = this.createSampleGuest();
        
        try {
            FiniteDuration finiteDuration = new FiniteDuration(20, SECONDS);
            
            guestAccountService.createAccount()
                    .invoke(sampleGuest)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            
            GuestEvent createGuestEvent = probe.request(1).expectNext(finiteDuration);
            assertTrue(createGuestEvent instanceof GuestEvent.AccountCreated);
            
            guestAccountService.updateAccount("successful@domain.com")
                    .invoke(sampleGuest)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            
            GuestEvent updateGuestEvent = probe.request(1).expectNext(finiteDuration);
            assertTrue(updateGuestEvent instanceof GuestEvent.AccountUpdated);
            
        } catch (Exception e) {
            assertTrue("The service thrown an exception.", e.equals(null));
        }
    }
    
    private Guest createSampleGuest() {
        return Guest.builder()
                .email("sample@email.com")
                .firstName("John")
                .lastName("Downs")
                .consumerId("123456789")
                .password("password!".toCharArray())
                .birthdate("19910101")
                .securityQuestions(Arrays.asList(SecurityQuestion.builder().question("what?").answer("yes").build()))
                .termsAndConditionsAgreement(TermsAndConditionsAgreement.builder().acceptTime("20170627033735PM").version("1.0").build())
                .optins(Arrays.asList(Optin.builder().type("EMAIL").flag(true).acceptTime("20170706022122PM").build()))
                .build();
    }
}
