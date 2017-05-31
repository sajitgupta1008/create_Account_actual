package com.rccl.middleware.guest.pingfederate;

import com.lightbend.lagom.javadsl.api.transport.HeaderFilter;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.ResponseHeader;

public class PingFederateHeaderFilter implements HeaderFilter {
    
    static final PingFederateHeaderFilter INSTANCE = new PingFederateHeaderFilter();
    
    private PingFederateHeaderFilter() {
        // No-op.
    }
    
    @Override
    public RequestHeader transformClientRequest(RequestHeader requestHeader) {
        return requestHeader.withHeader("AppKey", "api-key-will-go-here");
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
