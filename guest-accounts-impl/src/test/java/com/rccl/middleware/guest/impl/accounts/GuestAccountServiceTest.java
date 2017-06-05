package com.rccl.middleware.guest.impl.accounts;

import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.guest.impl.saviynt.SaviyntServiceImplStub;
import com.rccl.middleware.guest.saviynt.SaviyntService;
import com.rccl.middleware.guest.saviynt.exceptions.SaviyntExceptionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GuestAccountServiceTest {
    
    private static volatile TestServer testServer;
    
    private static GuestAccountService service;
    
    private static HeaderServiceCall<Guest, String> createAccount;
    
    @BeforeClass
    public static void beforeClass() {
        testServer = startServer(defaultSetup()
                .withCassandra(true)
                .configureBuilder(builder -> builder.overrides(
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(GuestAccountService.class).to(GuestAccountServiceImpl.class)
                ))
        );
        
        service = testServer.client(GuestAccountService.class);
        createAccount = (HeaderServiceCall<Guest, String>) service.createAccount();
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
    }
    
    @Test
    public void testPostGuestAccount() throws Exception {
        Guest guest = createSampleGuest().build();
        
        Pair<ResponseHeader, String> response = createAccount.invokeWithHeaders(RequestHeader.DEFAULT, guest).toCompletableFuture().get(5, SECONDS);
        
        assertTrue("The status code for success should be 201 Created.", response.first().status() == 201);
        assertEquals(guest.getEmail(), response.second());
    }
    
    @Test
    public void testPostGuestAccountFailureWithExistingEmail() {
        Guest guest = createSampleGuest().email("existing@email.com").build();
        
        try {
            createAccount.invokeWithHeaders(RequestHeader.DEFAULT, guest).toCompletableFuture().get(5, SECONDS);
        } catch (Exception e) {
            assertTrue("The exception should be an instance of SaviyntExceptionFactory.ExistingGuestException.", e instanceof SaviyntExceptionFactory.ExistingGuestException);
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
        TermsAndConditionsAgreement emptyVersion = TermsAndConditionsAgreement.builder().acceptTime(0L).build();
        Guest emptyVersionGuest = createSampleGuest().termsAndConditionsAgreement(emptyVersion).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyVersionGuest);
        
        // Test missing agreement time.
        TermsAndConditionsAgreement emptyAcceptTime = TermsAndConditionsAgreement.builder().version("1.0").acceptTime(null).build();
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
        Guest emptyQuestionGuest = createSampleGuest().securityQuestions(Arrays.asList(emptyQuestion)).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyQuestionGuest);
        
        // Test an empty answer field.
        SecurityQuestion emptyAnswer = SecurityQuestion.builder().question("question").answer("").build();
        Guest emptyAnswerGuest = createSampleGuest().securityQuestions(Arrays.asList(emptyAnswer)).build();
        shorthandInvokeExpectingWithValidationExceptionMessage(emptyAnswerGuest);
    }
    
    private Map<String, String> shorthandInvokeExpectingWithValidationExceptionMessage(Guest guest) {
        try {
            createAccount.invokeWithHeaders(RequestHeader.DEFAULT, guest);
        } catch (Exception e) {
            assertTrue("The exception should be of type InvalidGuestException.", e instanceof InvalidGuestException);
            
            InvalidGuestException ige = (InvalidGuestException) e;
            
            assertTrue("The exception's status code should be 422.", ige.exceptionMessage().getStatusCode() == 422);
            
            Map<String, String> validationErrors = ige.exceptionMessage().getValidationErrors();
            
            assertNotNull("The validation errors object should not be null.", validationErrors);
            
            assertFalse("The validation errors map should not be empty.", validationErrors.isEmpty());
            
            return ige.exceptionMessage().getValidationErrors();
        }
        
        throw new RuntimeException("This test should've returned in the catch block above.");
    }
    
    private Guest.GuestBuilder createSampleGuest() {
        Guest.GuestBuilder builder = Guest.builder();
        
        builder.firstName("Brad")
                .lastName("Pitt")
                .email("lawn@order.com")
                .password("Secret123".toCharArray())
                .brand('R');
        
        TermsAndConditionsAgreement tca = TermsAndConditionsAgreement.builder()
                .acceptTime(1490108091640L)
                .version("1.0")
                .build();
        
        builder.termsAndConditionsAgreement(tca);
        
        SecurityQuestion sq1 = SecurityQuestion.builder()
                .question("What is your mother's phone number?").answer("8675309")
                .build();
        
        SecurityQuestion sq2 = SecurityQuestion.builder()
                .question("What is the make and model of your first car?").answer("Knight Rider")
                .build();
        
        builder.securityQuestions(Arrays.asList(sq1, sq2));
        
        return builder;
    }
}
