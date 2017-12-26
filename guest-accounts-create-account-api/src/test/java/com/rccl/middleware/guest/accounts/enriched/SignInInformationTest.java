package com.rccl.middleware.guest.accounts.enriched;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SignInInformationTest {
    
    @Test
    public void testGetterForPasswordReturnsClonedPasswordArray() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        SignInInformation frc = SignInInformation.builder()
                .password(password)
                .build();
        
        char[] actualPassword = frc.getPassword();
        
        assertNotEquals(password, actualPassword);
        assertEquals(ArrayUtils.toString(password), ArrayUtils.toString(actualPassword));
    }
    
    @Test
    public void testGetterForPasswordAlwaysReturnsNewArrayInstance() {
        char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
        
        SignInInformation frc = SignInInformation.builder()
                .password(password)
                .build();
        
        char[] instanceOne = frc.getPassword();
        char[] instanceTwo = frc.getPassword();
        
        assertNotEquals(instanceOne, instanceTwo);
        assertEquals(ArrayUtils.toString(instanceOne), ArrayUtils.toString(instanceTwo));
    }
}
