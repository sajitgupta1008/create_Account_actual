package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.header.Header;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.Email;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountCredentials implements Jsonable {
    
    @NotNull(message = "A header is required.")
    @Valid
    Header header;
    
    @NotNull(message = "A username is required.")
    @Email(regexp = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
            message = "The email is invalidly formatted.")
    String username;
    
    @NotNull(message = "A password is required.")
    char[] password;
}
