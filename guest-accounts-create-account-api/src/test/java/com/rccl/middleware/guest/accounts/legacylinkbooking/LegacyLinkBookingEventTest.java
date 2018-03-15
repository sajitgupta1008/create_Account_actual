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

public class LegacyLinkBookingEventTest {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Test
    public void testGetters() {
        LegacyLinkBookingEvent.LegacyAccountLinked event = new LegacyLinkBookingEvent.LegacyAccountLinked(null, null, null, null);
        
        assertNull(event.getBrand());
        assertNull(event.getGuest());
        assertNull(event.getReservationUserIds());
        assertNull(event.getWebshopperIds());
    }
    
    @Test
    public void testSerialization() {
        LegacyLinkBookingEvent event = new LegacyLinkBookingEvent.LegacyAccountLinked(
                Guest.builder().build(),
                Collections.emptyList(),
                Collections.emptyList(),
                "R");
        
        JsonNode json = OBJECT_MAPPER.valueToTree(event);
        
        int expectedNumberOfProperties = 5;
        int actualNumberOfProperties = json.size();
        assertEquals(expectedNumberOfProperties, actualNumberOfProperties);
        
        assertTrue(json.has("type"));
        assertEquals("legacyAccountLinked", json.get("type").textValue());
        
        assertTrue(json.has("guest"));
        assertTrue(Guest.builder().build().equals(OBJECT_MAPPER.convertValue(json.get("guest"), Guest.class)));
        
        assertTrue(json.has("webshopperIds"));
        assertEquals(Collections.emptyList(), OBJECT_MAPPER.convertValue(json.get("webshopperIds"), List.class));
        
        assertTrue(json.has("reservationUserIds"));
        assertEquals(Collections.emptyList(), OBJECT_MAPPER.convertValue(json.get("reservationUserIds"), List.class));
        
        assertTrue(json.has("brand"));
        assertEquals("R", json.get("brand").textValue());
    }
    
    @Test
    public void testDeserialization() throws IOException {
        String jsonText = "{"
                + "\"type\":\"legacyAccountLinked\","
                + "\"guest\":null,"
                + "\"webshopperIds\":[],"
                + "\"reservationUserIds\":[],"
                + "\"brand\":\"Z\"}";
        
        LegacyLinkBookingEvent.LegacyAccountLinked event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingEvent.LegacyAccountLinked.class);
        
        assertEquals("Z", event.getBrand());
        
        assertNull(event.getGuest());
        
        assertNotNull(event.getReservationUserIds());
        assertTrue(event.getReservationUserIds().isEmpty());
        
        assertNotNull(event.getWebshopperIds());
        assertTrue(event.getWebshopperIds().isEmpty());
    }
    
    @Test
    public void testDeserializationWithMissingProperties() throws IOException {
        String jsonText = "{\"type\":\"legacyAccountLinked\"}";
        
        LegacyLinkBookingEvent.LegacyAccountLinked event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingEvent.LegacyAccountLinked.class);
        
        assertNull(event.getBrand());
        
        assertNull(event.getGuest());
        
        assertNull(event.getReservationUserIds());
        
        assertNull(event.getWebshopperIds());
    }
    
    @Test
    public void testDeserializationWithMissingType() throws IOException {
        String jsonText = "{}";
        LegacyLinkBookingEvent event = OBJECT_MAPPER.readValue(jsonText, LegacyLinkBookingEvent.class);
        assertNull(event);
    }
}
