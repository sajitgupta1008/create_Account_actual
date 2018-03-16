package com.rccl.middleware.guest.impl.accounts;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.rccl.middleware.vds.VDSService;
import com.rccl.middleware.vds.VDSServiceStub;
import com.rccl.middleware.vds.requests.PatchVDSVirtualID;
import com.rccl.middleware.vds.responses.GenericVDSResponse;
import com.rccl.middleware.vds.responses.WebShopperView;
import com.rccl.middleware.vds.responses.WebShopperViewList;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GuestAccountsVDSHelperTest {
    
    @Test
    public void testSetAllMatchingWebShopperIdsAsMigrated() {
        String expectedWebshopperId = "1234567";
        String expectedEmail = "webshopper@email.com";
        String expectedVdsId = "G1234567";
        
        Map<String, Integer> count = new HashMap<>();
        count.put("count", 0);
        
        VDSService vdsService = new VDSServiceStub() {
            
            @Override
            public ServiceCall<PatchVDSVirtualID, GenericVDSResponse> patchVDSVirtualId(String filter) {
                return patchVDSVirtualID -> {
                    assertEquals("vdsid=" + expectedWebshopperId + ",ou=shopper,dc=rccl,dc=com", filter);
                    
                    count.put("count", count.get("count") + 1);
                    
                    GenericVDSResponse response = GenericVDSResponse.builder().status(200).build();
                    return CompletableFuture.completedFuture(response);
                };
            }
            
            @Override
            public ServiceCall<NotUsed, WebShopperViewList> getWebShopperAttributes(String filter) {
                return notUsed -> {
                    WebShopperView resultOne = WebShopperView.builder()
                            .webshopperId(expectedWebshopperId)
                            .webshopperUsername(expectedEmail)
                            .reservationUserId("webShopperUserId")
                            .email(expectedEmail)
                            .build();
                    
                    WebShopperViewList webShopperViewList = WebShopperViewList.builder()
                            .webshopperViews(Collections.singletonList(resultOne))
                            .build();
                    
                    return CompletableFuture.completedFuture(webShopperViewList);
                };
            }
        };
        
        GuestAccountsVDSHelper helper = new GuestAccountsVDSHelper(vdsService);
        
        List<WebShopperView> views = helper.setAllMatchingWebShopperIdsAsMigrated(expectedVdsId, expectedEmail)
                .toCompletableFuture()
                .join();
        
        assertNotNull(views);
        
        int expectedNumberOfViews = 1;
        int actualNumberOfViews = views.size();
        assertEquals(expectedNumberOfViews, actualNumberOfViews);
        
        for (WebShopperView view : views) {
            assertEquals(expectedEmail, view.getWebshopperUsername());
        }
        
        int expectedNumberOfMigrationCalls = 1;
        int actualNumberOfMigrationCalls = count.get("count");
        assertEquals(expectedNumberOfMigrationCalls, actualNumberOfMigrationCalls);
    }
}
