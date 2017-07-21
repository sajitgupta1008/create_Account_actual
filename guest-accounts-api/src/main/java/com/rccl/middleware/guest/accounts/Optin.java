package com.rccl.middleware.guest.accounts;

import com.rccl.middleware.common.validation.validator.DateFormat;
import com.rccl.middleware.guest.optin.validation.ValidOptin;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class Optin {
    
    @ValidOptin(groups = Guest.DefaultChecks.class)
    String type;
    
    boolean flag;
    
    @NotNull(message = "The acceptance time in ISO-8601 date format (yyyyMMddhhmmssaa) is required.", groups = Guest.DefaultChecks.class)
    @DateFormat(format = "yyyyMMddhhmmssaa", message = "The date must follow ISO-8601 format(yyyyMMddhhmmssaa).", groups = Guest.DefaultChecks.class)
    String acceptTime;
}
