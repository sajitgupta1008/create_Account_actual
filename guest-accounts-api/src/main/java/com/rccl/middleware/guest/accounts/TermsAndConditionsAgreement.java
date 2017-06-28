package com.rccl.middleware.guest.accounts;

import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.validation.DateFormat;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class TermsAndConditionsAgreement implements Jsonable {
    
    @NotNull(message = "The acceptance time in ISO-8601 date format (yyyyMMddHHmmssaa) is required.", groups = Guest.DefaultChecks.class)
    @DateFormat(format = "yyyyMMddHHmmssaa",
            message = "The date must follow ISO-8601 format (yyyyMMddHHmmssaa).", groups = Guest.DefaultChecks.class)
    String acceptTime;
    
    @NotNull(message = "The terms and conditions version number is required.", groups = Guest.DefaultChecks.class)
    String version;
}
