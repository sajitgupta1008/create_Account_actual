package com.rccl.middleware.guest.accounts;

import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.DateFormat;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class TermsAndConditionsAgreement implements Jsonable {
    @NotBlank(message = "The acceptance time in ISO-8601 date format (yyyyMMddHHmmssaa) is required.", groups = Guest.CreateChecks.class)
    @NotNull(message = "The acceptance time in ISO-8601 date format (yyyyMMddHHmmssaa) is required.", groups = Guest.UpdateChecks.class)
    @DateFormat(format = "yyyyMMddHHmmssaa", message = "The date must follow ISO-8601 format(yyyyMMddHHmmssaa).", groups = Guest.DefaultChecks.class)
    String acceptTime;
    
    @NotBlank(message = "The terms and conditions version number is required.", groups = Guest.CreateChecks.class)
    @NotNull(message = "The terms and conditions version number is required.", groups = Guest.UpdateChecks.class)
    String version;
}
