package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.GuestAccountPassword;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.Valid;
import java.util.List;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SignInInformation implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @GuestAccountPassword
    char[] password;
    
    @Valid
    List<SecurityQuestion> securityQuestions;
    
    public char[] getPassword() {
        if (!ArrayUtils.isEmpty(this.password)) {
            return this.password.clone();
        }
        
        return SignInInformationBuilder.NULL_PASSWORD;
    }
    
    public static class SignInInformationBuilder {
        
        static final char[] NULL_PASSWORD = null;
        
        public SignInInformationBuilder password(char[] password) {
            if (password != null) {
                this.password = password.clone();
            } else {
                this.password = NULL_PASSWORD;
            }
            
            return this;
        }
    }
}
