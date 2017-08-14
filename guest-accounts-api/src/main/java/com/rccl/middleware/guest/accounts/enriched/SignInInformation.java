package com.rccl.middleware.guest.accounts.enriched;

import com.rccl.middleware.common.validation.validator.GuestAccountPassword;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import java.util.List;

@Builder
@Value
public class SignInInformation {
    
    @GuestAccountPassword
    char[] password;
    
    @Valid
    List<SecurityQuestion> securityQuestions;
}
