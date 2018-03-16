package com.rccl.middleware.guest.accounts.legacylinkbooking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rccl.middleware.guest.accounts.Guest;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LegacyLinkBookingMessageTest {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Test
    public void testGetters() {
        LegacyLinkBookingMessage event = new LegacyLinkBookingMessage(null, null, null, null, null);
        
        assertNull(event.getBrand());
        assertNull(event.getConsumerIds());
        assertNull(event.getGuest());
        assertNull(event.getReservationUserIds());
        assertNull(event.getWebshopperIds());
    }
    
    @Test
    public void testSerialization() {
        LegacyLinkBookingMessage event = new LegacyLinkBookingMessage(
                "R",
                Collections.emptyList(),
                Guest.builder().build(),
                Collections.emptyList(),
                Collections.emptyList());
        
        JsonNode json = OBJECT_MAPPER.valueToTree(event);
        
        int expectedNumberOfProperties = 6;
        int actualNumberOfProperties = json.size();
        assertEquals(expectedNumberOfProperties, actualNumberOfProperties);
        
        assertTrue(json.has("brand"));
        assertEquals("R", json.get("brand").textValue());
        
        assertTrue(json.has("consumerIds"));
        assertEquals(Collections.emptyList(), OBJECT_MAPPER.convertValue(json.get("consumerIds"), List.class));
        
        assertTrue(json.has("guest"));
        assertTrue(Guest.builder().build().equals(OBJECT_MAPPER.convertValue(json.get("guest"), Guest.class)));
        
        assertTrue(json.has("reservationUserIds"));
        assertEquals(Collections.emptyList(), OBJECT_MAPPER.convertValue(json.get("reservationUserIds"), List.class));
        
        assertTrue(json.has("webshopperIds"));
        assertEquals(Collections.emptyList(), OBJECT_MAPPER.convertValue(json.get("webshopperIds"), List.class));
        
        assertTrue(json.has("type"));
        assertEquals("legacyAccountLinked", json.get("type").textValue());
    }
    
    @Test
    public void testDeserialization() throws IOException {
        String jsonText = "{"
                + "\"type\":\"legacyAccountLinked\","
                + "\"guest\":null,"
                + "\"consumerIds\":[],"
                + "\"webshopperIds\":[],"
                + "\"reservationUserIds\":[],"
                + "\"brand\":\"Z\"}";
        
        LegacyLinkBookingMessage event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingMessage.class);
        
        assertEquals("Z", event.getBrand());
        
        assertNull(event.getGuest());
        
        assertNotNull(event.getConsumerIds());
        assertTrue(event.getConsumerIds().isEmpty());
        
        assertNotNull(event.getReservationUserIds());
        assertTrue(event.getReservationUserIds().isEmpty());
        
        assertNotNull(event.getWebshopperIds());
        assertTrue(event.getWebshopperIds().isEmpty());
    }
    
    @Test
    public void testDeserializationWithMissingProperties() throws IOException {
        String jsonText = "{\"type\":\"legacyAccountLinked\"}";
        
        LegacyLinkBookingMessage event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingMessage.class);
        
        assertNull(event.getBrand());
        
        assertNull(event.getConsumerIds());
        
        assertNull(event.getGuest());
        
        assertNull(event.getReservationUserIds());
        
        assertNull(event.getWebshopperIds());
    }
    
    @Test
    public void testDeserializationWithMissingType() throws IOException {
        String jsonText = "{}";
        LegacyLinkBookingMessage event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingMessage.class);
        assertNull(event);
    }
}
