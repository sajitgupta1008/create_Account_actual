package com.rccl.middleware.guest.impl.saviynt;

import com.rccl.middleware.guest.saviynt.SaviyntGuest;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SaviyntGuestTest {
    
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor[] sgConstructors = SaviyntGuest.class.getDeclaredConstructors();
        
        assertTrue("The class should only have one constructor.", sgConstructors.length == 1);
        
        Constructor sgConstructor = sgConstructors[0];
        assertFalse("The class' constructor should be inaccessible.", sgConstructor.isAccessible());
        
        ArrayList<Object> o = new ArrayList<>();
        
        for (int i = 0; i < sgConstructor.getParameterCount(); i += 1) {
            o.add(null);
        }
        
        sgConstructor.setAccessible(true);
        Class instanceClass = sgConstructor.newInstance(o.toArray()).getClass();
        assertTrue("The constructor should return an instance of the proper type.", instanceClass == SaviyntGuest.class);
    }
    
    @Test
    public void testNoArgBuilder() {
        SaviyntGuest sg = getNoArgInstance();
        
        assertNull("The no-arg instance should have a null first name.", sg.getFirstname());
        assertNull("The no-arg instance should have a null last name.", sg.getLastname());
        assertNull("The no-arg instance should have a null username.", sg.getUsername());
        assertNull("The no-arg instance should have a null email.", sg.getEmail());
        assertNull("The no-arg instance should have a null password.", sg.getPassword());
        assertNull("The no-arg instance should have a null security question.", sg.getSecurityquestion());
        assertNull("The no-arg instance should have a null security answer.", sg.getSecurityanswer());
        
        assertTrue("The no-arg instance should have a propertytosearch value of \"email\".", "email".equals(sg.getPropertytosearch()));
    }
    
    private SaviyntGuest getNoArgInstance() {
        return SaviyntGuest.builder().build();
    }
}
