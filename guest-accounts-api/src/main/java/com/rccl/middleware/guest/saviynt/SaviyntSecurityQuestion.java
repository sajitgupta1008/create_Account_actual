package com.rccl.middleware.guest.saviynt;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaviyntSecurityQuestion {
    
    String answer;
    
    String question;
}
