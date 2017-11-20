package com.rccl.middleware.guest.accounts;

import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.DateFormat;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class PrivacyPolicyAgreement implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "The privacy policy accept time in ISO-8601 date format (yyyyMMddThhmmssz) is required.",
            groups = Guest.CreateChecks.class)
    @NotNull(message = "The privacy policy accept time in ISO-8601 date format (yyyyMMddThhmmssz) is required.",
            groups = Guest.UpdateChecks.class)
    @DateFormat(format = "yyyyMMdd'T'HHmmssz", message = "The privacy policy accept time must follow ISO-8601 format: yyyyMMddThhmmssz",
            groups = Guest.DefaultChecks.class)
    String acceptTime;
    
    @NotBlank(message = "The privacy policy version is required.",
            groups = Guest.CreateChecks.class)
    @NotNull(message = "The privacy policy version is required.",
            groups = Guest.UpdateChecks.class)
    String version;
}
