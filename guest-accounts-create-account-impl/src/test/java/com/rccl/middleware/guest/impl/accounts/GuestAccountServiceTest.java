package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer;
import com.rccl.middleware.akka.clustermanager.models.ActorSystemInformation;
import com.rccl.middleware.common.exceptions.MiddlewareError;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.request.EnvironmentDetails;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.common.validation.MiddlewareValidationException;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.PrivacyPolicyAgreement;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.authentication.GuestAuthenticationService;
import com.rccl.middleware.guest.authentication.GuestAuthenticationServiceStub;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.guestprofiles.GuestProfileServiceStub;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import com.rccl.middleware.vds.VDSService;
import com.rccl.middleware.vds.VDSServiceStub;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GuestAccountServiceTest {
    
    private static ActorSystem system;
    
    private static volatile TestServer testServer;
    
    private static GuestAccountService service;
    
    private static HeaderServiceCall<Guest, ResponseBody<JsonNode>> createAccount;
    
    @BeforeClass
    public static void beforeClass() {
        testServer = startServer(defaultSetup()
                .withCassandra(true)
                .configureBuilder(builder -> builder.overrides(
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(VDSService.class).to(VDSServiceStub.class),
                        bind(GuestAuthenticationService.class).to(GuestAuthenticationServiceStub.class),
                        bind(GuestAccountService.class).to(GuestAccountServiceImpl.class),
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class),
                        bind(GuestProfilesService.class).to(GuestProfileServiceStub.class)
                ))
        );
        
        service = testServer.client(GuestAccountService.class);
        createAccount = (HeaderServiceCall<Guest, ResponseBody<JsonNode>>) service.createAccount();
        system = ActorSystem.create();
    }
    
    @AfterClass
    public static void afterClass() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
        }
        
        if (service != null) {
            service = null;
        }
        
        system.terminate();
        system = null;
    }
    
    @Test
    public void testPostGuestAccount() throws Exception {
        Guest guest = createSampleGuest().build();
        
        Pair<ResponseHeader, ResponseBody<JsonNode>> responseBody = createAccount
                .invokeWithHeaders(RequestHeader.DEFAULT
                                .withHeader(EnvironmentDetails.ENVIRONMENT_MARKER_HEADER_NAME, "shore")
                                .withHeader(EnvironmentDetails.ENVIRONMENT_SHIP_CODE_HEADER_NAME, "none"),
                        guest).toCompletableFuture().get(5, SECONDS);
        JsonNode response = responseBody.second().getPayload();
        
        assertTrue("The status code for success should be 201 Created.",
                responseBody.first().status() == 201);
        assertEquals("G1234567", response.get("vdsId").asText());
        assertTrue(response.get("accessToken") != null);
        assertTrue(response.get("openIdToken") != null);
        assertTrue(response.get("refreshToken") != null);
    }
    
    @Test
    public void testPostGuestAccountFailureWithExistingEmail() {
        Guest guest = createSampleGuest().email("existing@email.com").build();
        
        try {
            createAccount.invokeWithHeaders(RequestHeader.DEFAULT, guest).toCompletableFuture().get(5, SECONDS);
        } catch (Exception e) {
            assertTrue("The exception should be an instance of "
                            + "SaviyntExceptionFactory.ExistingGuestException.",
                    e instanceof SaviyntExceptionFactory.ExistingGuestException);
        }
    }
    
    @Test
    public void testFailureWithMissingFirstName() {
        Guest blankFirstName = createSampleGuest().firstName("").build();
        shorthandInvokeExpectingWithValidationExceptionMessage(blankFirstName);
        
        Guest nullFirstName = createSampleGuest().firstName(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullFirstName);
    }
    
    @Test
    public void testFailureWithMissingLastName() {
        Guest blankLastName = createSampleGuest().lastName(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(blankLastName);
        
        Guest nullLastName = createSampleGuest().lastName("").build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullLastName);
    }
    
    @Test
    public void testValidationFailuresWithEmail() {
        Guest blankEmail = createSampleGuest().email("").build();
        shorthandInvokeExpectingWithValidationExceptionMessage(blankEmail);
        
        Guest nullEmail = createSampleGuest().email(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullEmail);
        
        Guest invalidFormatEmail = createSampleGuest().email("testRedundantEmail@rccl").build();
        shorthandInvokeExpectingWithValidationExceptionMessage(invalidFormatEmail);
    }
    
    @Test
    public void testFailureWithMissingPassword() {
        Guest blankPassword = createSampleGuest().password("".toCharArray()).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(blankPassword);
        
        Guest nullPassword = createSampleGuest().password(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullPassword);
    }
    
    @Test
    public void testFailureWithInvalidTermsAndConditionsAgreement() {
        // Test null agreement.
        Guest nullTACA = createSampleGuest().termsAndConditionsAgreement(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullTACA);
        
        // Test missing agreement version.
        TermsAndConditionsAgreement emptyVersion = TermsAndConditionsAgreement.builder().acceptTime("").build();
        Guest emptyVersionGuest = createSampleGuest().termsAndConditionsAgreement(emptyVersion).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyVersionGuest);
        
        // Test missing agreement time.
        TermsAndConditionsAgreement emptyAcceptTime = TermsAndConditionsAgreement.builder()
                .version("1.0").acceptTime(null).build();
        Guest emptyAcceptTimeGuest = createSampleGuest().termsAndConditionsAgreement(emptyAcceptTime).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyAcceptTimeGuest);
    }
    
    @Test
    public void testFailureWithInvalidSecurityQuestions() {
        // test null security questions list.
        Guest nullSQ = createSampleGuest().securityQuestions(null).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(nullSQ);
        
        // Test empty security questions list.
        Guest emptySQ = createSampleGuest().securityQuestions(new ArrayList<>()).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptySQ);
        
        // Test an empty question field.
        SecurityQuestion emptyQuestion = SecurityQuestion.builder().question("").answer("answer").build();
        Guest emptyQuestionGuest = createSampleGuest()
                .securityQuestions(Collections.singletonList(emptyQuestion))
                .build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyQuestionGuest);
        
        // Test an empty answer field.
        SecurityQuestion emptyAnswer = SecurityQuestion.builder().question("question").answer("").build();
        Guest emptyAnswerGuest = createSampleGuest()
                .securityQuestions(Collections.singletonList(emptyAnswer))
                .build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyAnswerGuest);
    }
    
    private List<MiddlewareError> shorthandInvokeExpectingWithValidationExceptionMessage(Guest guest) {
        try {
            createAccount.invokeWithHeaders(RequestHeader.DEFAULT, guest);
        } catch (Exception e) {
            assertTrue("The exception should be of type InvalidGuestException.",
                    e instanceof MiddlewareValidationException);
            
            MiddlewareValidationException ige = (MiddlewareValidationException) e;
            
            assertTrue("The exception's status code should be 422.",
                    ige.exceptionMessage().getStatus() == 422);
            
            List<MiddlewareError> validationErrors = ige.exceptionMessage().getErrors();
            
            assertNotNull("The validation errors object should not be null.", validationErrors);
            
            assertFalse("The validation errors map should not be empty.", validationErrors.isEmpty());
            
            return ige.exceptionMessage().getErrors();
        }
        
        throw new RuntimeException("This test should've returned in the catch block above.");
    }
    
    private Guest.GuestBuilder createSampleGuest() {
        Guest.GuestBuilder builder = Guest.builder();
        
        builder.header(Header.builder()
                .channel("app-ios")
                .brand('R')
                .locale(Locale.US)
                .build());
        
        builder.firstName("Brad")
                .lastName("Pitt")
                .email("successful@domain.com")
                .birthdate("19910101")
                .phoneNumber("+1(123)-234-9867")
                .password("secretpass1".toCharArray());
        
        TermsAndConditionsAgreement tca = TermsAndConditionsAgreement.builder()
                .acceptTime("20170627T033735UTC")
                .version("1.0")
                .build();
        
        builder.termsAndConditionsAgreement(tca);
        
        PrivacyPolicyAgreement ppa = PrivacyPolicyAgreement.builder()
                .acceptTime("20170627T033735UTC")
                .version("1.0")
                .build();
        
        builder.privacyPolicyAgreement(ppa);
        
        SecurityQuestion sq1 = SecurityQuestion.builder()
                .question("What is your mother's phone number?").answer("8675309")
                .build();
        
        SecurityQuestion sq2 = SecurityQuestion.builder()
                .question("What is the make and model of your first car?").answer("Knight Rider")
                .build();
        
        builder.securityQuestions(Arrays.asList(sq1, sq2));
        
        return builder;
    }
    
    @Test
    public void testAkkaHealthCheck() throws Exception {
        HeaderServiceCall<NotUsed, ResponseBody<ActorSystemInformation>> healthCheckService =
                (HeaderServiceCall<NotUsed, ResponseBody<ActorSystemInformation>>) service.akkaClusterHealthCheck();
        
        Pair<ResponseHeader, ResponseBody<ActorSystemInformation>> response = healthCheckService
                .invokeWithHeaders(RequestHeader.DEFAULT, NotUsed.getInstance())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        
        ActorSystemInformation payload = response.second().getPayload();
        
        assertNotNull(payload.getActorSystemName());
        assertNotNull(payload.getSelfAddress());
        assertFalse(payload.getClusterMembers().isEmpty());
    }
}
