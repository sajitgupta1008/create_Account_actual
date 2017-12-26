package com.rccl.middleware.guest.accounts;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class GuestTest {
    
    @Test
    public void testGetterForPasswordReturnsClonedPasswordArray() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        Guest guest = Guest.builder()
                .password(password)
                .build();
        
        char[] actualPassword = guest.getPassword();
        
        assertNotEquals(password, actualPassword);
        assertEquals(ArrayUtils.toString(password), ArrayUtils.toString(actualPassword));
    }
    
    @Test
    public void testGetterForPasswordAlwaysReturnsNewArrayInstance() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        Guest guest = Guest.builder()
                .password(password)
                .build();
        
        char[] instanceOne = guest.getPassword();
        char[] instanceTwo = guest.getPassword();
        
        assertNotEquals(instanceOne, instanceTwo);
        assertEquals(ArrayUtils.toString(instanceOne), ArrayUtils.toString(instanceTwo));
    }
    
    @Test
    public void testGetterHonorsEmptyArray() {
        Guest guest = Guest.builder()
                .password(new char[0])
                .build();
        
        assertNotNull(guest.getPassword());
    }
}
