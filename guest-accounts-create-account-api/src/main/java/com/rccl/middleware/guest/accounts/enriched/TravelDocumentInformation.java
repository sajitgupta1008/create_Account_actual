package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.DateFormat;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelDocumentInformation implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @JsonView(EnrichedGuest.ExtendedView.class)
    String passportNumber;
    
    @JsonView(EnrichedGuest.ExtendedView.class)
    @DateFormat
    String passportExpirationDate;
    
    @Pattern(regexp = "[A-Za-z]{3}", message = "The citizenship country code "
            + "is required to be the three-character country code.")
    String citizenshipCountryCode;
    
    @Pattern(regexp = "[A-Za-z]{3}", message = "The birth country code "
            + "is required to be the three-character country code.")
    String birthCountryCode;
}

