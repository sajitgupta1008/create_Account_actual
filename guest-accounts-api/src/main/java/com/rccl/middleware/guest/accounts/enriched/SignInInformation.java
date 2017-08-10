package com.rccl.middleware.guest.accounts.enriched;

import com.rccl.middleware.common.validation.validator.GuestAccountPassword;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.Email;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@Builder
@Value
public class SignInInformation {
    
    @Size(min = 5, max = 256, message = "The email can only have up to 256 characters.")
    @Email(regexp = ValidatorConstants.EMAIL_REGEXP, message = "The email is invalidly formatted.")
    String email;
    
    @GuestAccountPassword
    char[] password;
    
    @Valid
    List<SecurityQuestion> securityQuestions;
}
