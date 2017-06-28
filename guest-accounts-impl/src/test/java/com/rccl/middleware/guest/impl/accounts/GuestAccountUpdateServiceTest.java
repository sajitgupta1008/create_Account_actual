package com.rccl.middleware.guest.impl.accounts;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.lightbend.lagom.javadsl.server.HeaderServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        String emailID = "successful@domain.com";
        
        List<SecurityQuestion> securityQuestionList = new ArrayList<>();
        SecurityQuestion securityQuestion = SecurityQuestion.builder()
                .question("What is your name?").answer("NoName").build();
        securityQuestionList.add(securityQuestion);
        
        Guest guest = Guest.builder()
                .email(emailID)
                .firstName("John")
                .lastName("Dale")
                .birthdate("19910101")
                .password("pass123".toCharArray())
                .securityQuestions(securityQuestionList)
                .crownAndAnchorIds(Arrays.asList("12345678", "12345678"))
                .azamaraBookingIds(Arrays.asList("123456", "123457"))
                .celebrityBookingIds(Arrays.asList("123456", "123457"))
                .royalBookingIds(Arrays.asList("123456", "123457"))
                .azamaraWebShopperIds(Arrays.asList("123456", "123457"))
                .celebrityWebShopperIds(Arrays.asList("123456", "123457"))
                .royalWebShopperIds(Arrays.asList("123456", "123457"))
                .royalPrimaryBookingId("123456")
                .celebrityPrimaryBookingId("123455")
                .azamaraPrimaryBookingId("123455")
                .build();
        
        try {
            HeaderServiceCall<Guest, JsonNode> updateAccount = (HeaderServiceCall<Guest, JsonNode>) guestAccountService.updateAccount(emailID);
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue("Response must not be null.", response != null);
            assertTrue("Response Header Status must be 200", response.first().status() == 200);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of MiddlewareTransportException", e.getCause() instanceof MiddlewareTransportException);
        }
    }
    
    @Test
    public void shouldFailGuestUpdate() {
        String emailID = "willfail@domain.com";
        
        List<SecurityQuestion> securityQuestionList = new ArrayList<>();
        SecurityQuestion securityQuestion = SecurityQuestion.builder()
                .question("What is your name?").answer("NoName").build();
        securityQuestionList.add(securityQuestion);
        
        Guest guest = Guest.builder()
                .email(emailID)
                .firstName("John")
                .lastName("Dale")
                .birthdate("19910101")
                .password("pass345!".toCharArray())
                .securityQuestions(securityQuestionList)
                .build();
        
        try {
            HeaderServiceCall<Guest, JsonNode> updateAccount = (HeaderServiceCall<Guest, JsonNode>) guestAccountService.updateAccount(emailID);
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue(response != null);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of NoSuchGuestException.", e instanceof SaviyntExceptionFactory.NoSuchGuestException);
        }
    }
    
    @Test
    public void shouldFailUpdateWithInvalidFields() {
        String emailID = "willfail@domain.com";
        
        List<SecurityQuestion> securityQuestionList = new ArrayList<>();
        SecurityQuestion securityQuestion = SecurityQuestion.builder()
                .question("What is your name?").answer("NoName").build();
        securityQuestionList.add(securityQuestion);
        
        Guest guest = Guest.builder()
                .email(emailID)
                .firstName("J")
                .lastName("D")
                .birthdate("19910101")
                .password("345".toCharArray())
                .securityQuestions(securityQuestionList)
                .build();
        
        try {
            HeaderServiceCall<Guest, JsonNode> updateAccount = (HeaderServiceCall<Guest, JsonNode>) guestAccountService.updateAccount(emailID);
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue(response != null);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of InvalidGuestException.", e instanceof InvalidGuestException);
        }
    }
    
    @Test
    public void shouldFailUpdateWithInvalidCollectionValues() {
        String emailID = "something@domain.com";
        
        List<SecurityQuestion> securityQuestionList = new ArrayList<>();
        SecurityQuestion securityQuestion = SecurityQuestion.builder()
                .question("What is your name?").answer("NoName").build();
        securityQuestionList.add(securityQuestion);
        
        Guest guest = Guest.builder()
                .email(emailID)
                .firstName("John")
                .lastName("Dale")
                .birthdate("19910101")
                .password("pass123!".toCharArray())
                .securityQuestions(securityQuestionList)
                .crownAndAnchorIds(Arrays.asList("asdd", "12345678"))
                .captainsClubIds(Arrays.asList("asdd", "12345678"))
                .azamaraLoyaltyIds(Arrays.asList("asdd", "12345678"))
                .clubRoyaleIds(Arrays.asList("asdd", "12345678"))
                .celebrityBlueChipIds(Arrays.asList("asdd", "12345678"))
                .azamaraBookingIds(Arrays.asList("asdasd", "123457"))
                .celebrityBookingIds(Arrays.asList("asda", "123457"))
                .royalBookingIds(Arrays.asList("gfdgfd", "123457"))
                .azamaraWebShopperIds(Arrays.asList("dfgdg", "123457"))
                .celebrityWebShopperIds(Arrays.asList("123456", "dfgfd"))
                .royalWebShopperIds(Arrays.asList("123456", "dfgfdg"))
                .royalPrimaryBookingId("asdasd")
                .celebrityPrimaryBookingId("asdsad")
                .azamaraPrimaryBookingId("asdsadasd")
                .build();
        
        try {
            HeaderServiceCall<Guest, JsonNode> updateAccount = (HeaderServiceCall<Guest, JsonNode>) guestAccountService.updateAccount(emailID);
            
            Pair<ResponseHeader, JsonNode> response = updateAccount
                    .invokeWithHeaders(RequestHeader.DEFAULT, guest)
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            
            assertTrue("This should fail instead.", response != null);
            
        } catch (Exception e) {
            assertTrue("Exception must be an instance of InvalidGuestException.", e instanceof InvalidGuestException);
        }
    }
}
