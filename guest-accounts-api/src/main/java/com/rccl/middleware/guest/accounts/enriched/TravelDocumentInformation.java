package com.rccl.middleware.guest.accounts.enriched;

import com.rccl.middleware.common.validation.validator.DateFormat;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
public class TravelDocumentInformation {
    
    String passportNumber;
    
    @DateFormat(message = "The date must follow ISO-8601 format(yyyyMMdd).")
    String passportExpirationDate;
    
    @Pattern(regexp = "[A-Za-z]{3}", message = "The citizenship country code "
            + "is required to be the three-character country code.")
    String citizenshipCountryCode;
    
    @Pattern(regexp = "[A-Za-z]{3}", message = "The birth country code "
            + "is required to be the three-character country code.")
    String birthCountryCode;
}

