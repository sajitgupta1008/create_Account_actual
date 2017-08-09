package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rccl.middleware.guest.optin.Optins;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedGuest {
    
    @Valid
    PersonalInformation personalInformation;
    
    @Valid
    ContactInformation contactInformation;
    
    @Valid
    TravelDocumentInformation travelDocumentInformation;
    
    @Valid
    LoyaltyInformation loyaltyInformation;
    
    @Valid
    WebshopperInformation webshopperInformation;
    
    @Valid
    EmergencyContact emergencyContact;
    
    @Valid
    Optins optins;
    
    @NotNull(message = "A VDS ID is required.")
    String vdsId;
}
