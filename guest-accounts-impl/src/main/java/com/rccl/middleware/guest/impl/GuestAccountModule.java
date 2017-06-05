package com.rccl.middleware.guest.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.impl.accounts.GuestAccountServiceImpl;
import com.rccl.middleware.guest.saviynt.SaviyntService;

public class GuestAccountModule extends AbstractModule implements ServiceGuiceSupport {
    
    @Override
    protected void configure() {
        bindService(GuestAccountService.class, GuestAccountServiceImpl.class);
        bindClient(SaviyntService.class);
    }
}
