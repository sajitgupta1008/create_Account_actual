package com.rccl.middleware.guest.impl.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.testkit.ServiceTest.TestServer;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.forgerock.api.ForgeRockService;
import com.rccl.middleware.forgerock.api.ForgeRockServiceImplStub;
import com.rccl.middleware.guest.accounts.AccountCredentials;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guest.optin.GuestProfileOptinsStub;
import com.rccl.middleware.guestprofiles.GuestProfileServiceStub;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

@Ignore
public class GuestAccountLoginServiceTest {
    
    private static volatile TestServer testServer;
    
    private static GuestAccountService service;
    
    @BeforeClass
    public static void beforeClass() {
        testServer = startServer(defaultSetup()
                .withCassandra(true)
                .configureBuilder(builder -> builder.overrides(
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class),
                        bind(ForgeRockService.class).to(ForgeRockServiceImplStub.class),
                        bind(GuestProfileOptinService.class).to(GuestProfileOptinsStub.class),
                        bind(GuestProfilesService.class).to(GuestProfileServiceStub.class)
                ))
        );
        
        service = testServer.client(GuestAccountService.class);
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
    
    @Test(expected = ExecutionException.class)
    public void testWebAuthentication() throws Exception {
        AccountCredentials credentials = AccountCredentials.builder()
                .header(Header.builder().brand('R').channel("web").locale(Locale.US).build())
                .username("successful@domain.com")
                .password("password1".toCharArray())
                .build();
        
        service.authenticateUser().invoke(credentials)
                .toCompletableFuture().get(5, TimeUnit.SECONDS);
        
    }
    
    @Test
    public void testSuccessfulMobileAuthentication() {
        AccountCredentials credentials = AccountCredentials.builder()
                .header(Header.builder().brand('R').channel("app-ios").locale(Locale.US).build())
                .username("successful@domain.com")
                .password("password1".toCharArray())
                .build();
        
        try {
            JsonNode response = service.authenticateUser().invoke(credentials)
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
            
            assertTrue("Account login status must not be null", response.get("accountLoginStatus") != null);
            assertTrue("accessToken must not be null", response.get("accessToken") != null);
            assertTrue("refreshToken must not be null", response.get("refreshToken") != null);
            assertTrue("openIdToken must not be null", response.get("openIdToken") != null);
            assertTrue("vdsId must not be null", response.get("vdsId") != null);
            assertTrue("email must not be null", response.get("email") != null);
            assertTrue("firstName must not be null", response.get("firstName") != null);
            assertTrue("lastName must not be null", response.get("lastName") != null);
            
        } catch (Exception e) {
            assertFalse("Must return successful login instead.", e == null);
        }
    }
    
    @Test(expected = ExecutionException.class)
    public void testFailureAuthentication() throws Exception {
        AccountCredentials credentials = AccountCredentials.builder()
                .header(Header.builder().brand('R').channel("app-ios").locale(Locale.US).build())
                .username("failure@domain.com")
                .password("password!".toCharArray())
                .build();
        
        JsonNode response = service.authenticateUser().invoke(credentials)
                .toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        assertFalse("Must throw an exception instead", response != null);
    }
}
