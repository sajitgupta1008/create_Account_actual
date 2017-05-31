package com.rccl.middleware.guest.saviynt;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SaviyntSecurityQuestionAnswerValidation {
    
    String email;
    
    List<SaviyntSecurityQuestion> answers;
    
    final String propertytosearch = "email";
}
