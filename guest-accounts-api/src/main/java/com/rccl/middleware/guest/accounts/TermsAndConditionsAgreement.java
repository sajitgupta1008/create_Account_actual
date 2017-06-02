package com.rccl.middleware.guest.accounts;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class TermsAndConditionsAgreement implements Jsonable {
    
    @NotNull(message = "The acceptance time of the terms and conditions in milliseconds is required.", groups = Guest.DefaultChecks.class)
    Long acceptTime;
    
    @NotNull(message = "The terms and conditions version number is required.", groups = Guest.DefaultChecks.class)
    String version;
}
