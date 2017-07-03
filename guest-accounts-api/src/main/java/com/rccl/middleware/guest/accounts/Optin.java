package com.rccl.middleware.guest.accounts;

import com.rccl.middleware.common.validation.validator.DateFormat;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class Optin {
    
    String type;
    
    boolean flag;
    
    @NotNull(message = "The acceptance time in ISO-8601 date format (yyyyMMddHHmmssaa) is required.", groups = Guest.DefaultChecks.class)
    @DateFormat(format = "yyyyMMddHHmmssaa", message = "The date must follow ISO-8601 format(yyyyMMddHHmmssaa).", groups = Guest.DefaultChecks.class)
    String acceptTime;
}
