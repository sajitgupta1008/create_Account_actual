package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guestprofiles.models.Address;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactInformation implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    String phoneCountryCode;
    
    @Size(min = 7, max = 30, message = "The phone number must be at least seven (7) characters"
            + " and maximum of thirty (30) characters.")
    @Pattern(regexp = "[ 0-9+()-]*", message = "The phone number is invalidly formatted.")
    String phoneNumber;
    
    @Valid
    Address address;
}
