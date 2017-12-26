package com.rccl.middleware.guest.accounts.enriched;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class SignInInformationTest {
    
    @Test
    public void testGetterForPasswordReturnsClonedPasswordArray() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        SignInInformation sii = SignInInformation.builder()
                .password(password)
                .build();
        
        char[] actualPassword = sii.getPassword();
        
        assertNotEquals(password, actualPassword);
        assertEquals(ArrayUtils.toString(password), ArrayUtils.toString(actualPassword));
    }
    
    @Test
    public void testGetterForPasswordAlwaysReturnsNewArrayInstance() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        SignInInformation sii = SignInInformation.builder()
                .password(password)
                .build();
        
        char[] instanceOne = sii.getPassword();
        char[] instanceTwo = sii.getPassword();
        
        assertNotEquals(instanceOne, instanceTwo);
        assertEquals(ArrayUtils.toString(instanceOne), ArrayUtils.toString(instanceTwo));
    }
    
    @Test
    public void testGetterHonorsEmptyArray() {
        SignInInformation sii = SignInInformation.builder()
                .password(new char[0])
                .build();
        
        assertNotNull(sii.getPassword());
    }
}
