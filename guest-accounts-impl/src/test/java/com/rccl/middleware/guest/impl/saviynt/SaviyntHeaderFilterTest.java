package com.rccl.middleware.guest.impl.saviynt;

import com.lightbend.lagom.javadsl.api.transport.HeaderFilter;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;
import com.rccl.middleware.guest.saviynt.SaviyntHeaderFilter;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SaviyntHeaderFilterTest {
    
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor[] shfConstructors = SaviyntHeaderFilter.class.getDeclaredConstructors();
        
        assertTrue("The class should only have one constructor.", shfConstructors.length == 1);
        
        Constructor shfConstructor = shfConstructors[0];
        
        assertFalse("The class' constructor should be inaccessible.", shfConstructor.isAccessible());
        
        shfConstructor.setAccessible(true);
        Object[] args = new Object[0];
        SaviyntHeaderFilter instance = (SaviyntHeaderFilter) shfConstructor.newInstance(args);
        
        assertTrue("The instance's class should be proper..", instance.getClass() == SaviyntHeaderFilter.class);
        assertTrue("The instance should implement Lagom's HeaderFilter.", instance instanceof HeaderFilter);
    }
    
    @Test
    public void testSingletonInstance() throws Exception {
        Field field = SaviyntHeaderFilter.class.getDeclaredField("INSTANCE");
        
        assertNotNull("The filter should have a property called INSTANCE.", field);
        assertFalse("The instance property should be package-private.", field.isAccessible());
        
        int modifiers = field.getModifiers();
        
        boolean isStatic = Modifier.isStatic(modifiers);
        assertTrue("The instance property should be static.", isStatic);
        
        boolean isFinal = Modifier.isFinal(field.getModifiers());
        assertTrue("The instance property should be final.", isFinal);
        
        boolean isPackagePrivate = !Modifier.isPrivate(modifiers) &
                !Modifier.isProtected(modifiers) &
                !Modifier.isPublic(modifiers);
        
        assertTrue("The instance property should be package-private.", isPackagePrivate);
    }
    
    @Test
    public void testTransformClientRequest() throws Exception {
        SaviyntHeaderFilter instance = getInstance();
        
        RequestHeader original = RequestHeader.DEFAULT;
        RequestHeader updated = instance.transformClientRequest(original);
        
        assertNotEquals("The updated RequestHeader should be a different instance.", original, updated);
        assertTrue("The updated RequestHeader has the SAVUSERNAME.", updated.headers().containsKey("SAVUSERNAME"));
        assertTrue("The updated RequestHeader has the SAVPASSWORD.", updated.headers().containsKey("SAVPASSWORD"));
    }
    
    @Test
    public void testTransformServerRequest() throws Exception {
        SaviyntHeaderFilter instance = getInstance();
        
        RequestHeader original = RequestHeader.DEFAULT;
        RequestHeader updated = instance.transformServerRequest(original);
        
        assertEquals("The updated RequestHeader should be the same instance as the original.", original, updated);
    }
    
    @Test
    public void testTransformClientResponse() throws Exception {
        SaviyntHeaderFilter instance = getInstance();
        
        ResponseHeader original = ResponseHeader.OK;
        ResponseHeader updated = instance.transformClientResponse(original, RequestHeader.DEFAULT);
        
        assertEquals("The updated ResponseHeader should be the same instance as the original.", original, updated);
    }
    
    @Test
    public void testTransformServerResponse() throws Exception {
        SaviyntHeaderFilter instance = getInstance();
        
        ResponseHeader original = ResponseHeader.OK;
        ResponseHeader updated = instance.transformServerResponse(original, RequestHeader.DEFAULT);
        
        assertEquals("The updated ResponseHeader should be the same instance as the original.", original, updated);
    }
    
    private SaviyntHeaderFilter getInstance() throws Exception {
        Field field = SaviyntHeaderFilter.class.getDeclaredField("INSTANCE");
        
        field.setAccessible(true);
        
        return (SaviyntHeaderFilter) field.get(SaviyntHeaderFilter.class);
    }
}
