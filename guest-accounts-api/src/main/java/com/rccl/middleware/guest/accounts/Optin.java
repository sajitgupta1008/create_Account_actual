package com.rccl.middleware.guest.accounts;

import com.rccl.middleware.common.validation.validator.DateFormat;
import com.rccl.middleware.guest.optin.validation.ValidOptin;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Value
@Builder
public class Optin {
    
    @ValidOptin(groups = Guest.DefaultChecks.class)
    String type;
    
    @NotNull(message = "A flag is required.", groups = Guest.DefaultChecks.class)
    @Pattern(regexp = "[YNU]", message = "The value of the flag can either be of the following: Y, N or U.",
            groups = Guest.DefaultChecks.class)
    String flag;
    
    @NotNull(message = "The acceptance time in ISO-8601 date format (yyyyMMddTHHmmssz) is required.",
            groups = Guest.DefaultChecks.class)
    @DateFormat(format = "yyyyMMdd'T'HHmmssz", message = "The date must follow ISO-8601 format(yyyyMMddTHHmmssz).",
            groups = Guest.DefaultChecks.class)
    String acceptTime;
}
