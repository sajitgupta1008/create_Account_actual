package com.rccl.middleware.guest.impl.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.email.AemEmailServiceStub;
import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.guest.accounts.AccountStatusEnum;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.SaviyntServiceImplStub;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;

public class GuestAccountEmailValidationServiceTest {
    
    private static volatile ServiceTest.TestServer testServer;
    
    private static GuestAccountService guestAccountEmailValidationService;
    
    @BeforeClass
    public static void setUp() {
        final ServiceTest.Setup setup = defaultSetup()
                .configureBuilder(builder -> builder.overrides(
                        bind(AemEmailService.class).to(AemEmailServiceStub.class),
                        bind(SaviyntService.class).to(SaviyntServiceImplStub.class)
                ));
        
        testServer = startServer(setup.withCassandra(true));
        guestAccountEmailValidationService = testServer.client(GuestAccountService.class);
    }
    
    @AfterClass
    public static void tearDown() {
        if (testServer != null) {
            testServer.stop();
            testServer = null;
        }
    }
    
    @Test
    public void shouldReturnStatusAccountExists() throws Exception {
        ResponseBody<JsonNode> response = guestAccountEmailValidationService
                .validateEmail("successful@domain.com", Optional.of("username"))
                .invoke().toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertNotNull(response.getPayload().get("status"));
        assertTrue(response.getPayload().get("status").asText().equals(AccountStatusEnum.EXISTING.value()));
    }
    
    @Test(expected = ExecutionException.class)
    public void shouldReturnStatusAccountNonExisting() throws Exception {
        ResponseBody<JsonNode> response = guestAccountEmailValidationService
                .validateEmail("notexisting@domain.com", Optional.of("username"))
                .invoke().toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertNotNull(response.getPayload().get("status"));
        assertTrue(response.getPayload().get("status").asText().equals(AccountStatusEnum.DOES_NOT_EXIST.value()));
    }
    
    @Test(expected = ExecutionException.class)
    public void shouldReturnInvalidEmailFailureResponse() throws Exception {
        guestAccountEmailValidationService.validateEmail("this.is.@invalidemail", Optional.of("username"))
                .invoke().toCompletableFuture().get(5, TimeUnit.SECONDS);
        
    }
}
