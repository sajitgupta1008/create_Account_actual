package com.rccl.middleware.guest.saviynt;

import com.lightbend.lagom.javadsl.api.transport.HeaderFilter;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;

public class SaviyntHeaderFilter implements HeaderFilter {
    
    static final SaviyntHeaderFilter INSTANCE = new SaviyntHeaderFilter();
    
    private SaviyntHeaderFilter() {
        // No-op.
    }
    
    @Override
    public RequestHeader transformClientRequest(RequestHeader requestHeader) {
        return requestHeader
                .withHeader("SAVUSERNAME", "admin")
                .withHeader("SAVPASSWORD", "password");
    }
    
    @Override
    public RequestHeader transformServerRequest(RequestHeader requestHeader) {
        return requestHeader;
    }
    
    @Override
    public ResponseHeader transformServerResponse(ResponseHeader responseHeader, RequestHeader requestHeader) {
        return responseHeader;
    }
    
    @Override
    public ResponseHeader transformClientResponse(ResponseHeader responseHeader, RequestHeader requestHeader) {
        return responseHeader;
    }
}
