package com.rccl.middleware.guest.accounts;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Optin {
    
    String type;
    
    boolean flag;
}
