package com.rccl.middleware.guest.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.rccl.middleware.guest.accounts.GuestAccountService;
import com.rccl.middleware.guest.authentication.GuestAuthenticationService;
import com.rccl.middleware.guest.impl.accounts.GuestAccountServiceImpl;
import com.rccl.middleware.guest.optin.GuestProfileOptinService;
import com.rccl.middleware.guestprofiles.GuestProfilesService;
import com.rccl.middleware.saviynt.api.SaviyntService;

public class GuestAccountModule extends AbstractModule implements ServiceGuiceSupport {
    
    @Override
    protected void configure() {
        bindService(GuestAccountService.class, GuestAccountServiceImpl.class);
        bindClient(SaviyntService.class);
        bindClient(GuestProfilesService.class);
        bindClient(GuestProfileOptinService.class);
        bindClient(GuestAuthenticationService.class);
    }
}
