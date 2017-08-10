package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedGuest {
    
    @NotNull(message = "A header is required.")
    @Valid
    Header header;
    
    @Valid
    PersonalInformation personalInformation;
    
    @Valid
    ContactInformation contactInformation;
    
    @Valid
    SignInInformation signInInformation;
    
    @Valid
    TravelDocumentInformation travelDocumentInformation;
    
    @Valid
    LoyaltyInformation loyaltyInformation;
    
    @Valid
    WebshopperInformation webshopperInformation;
    
    @Valid
    EmergencyContact emergencyContact;
    
    @Valid
    List<Optin> optins;
    
    @NotNull(message = "A VDS ID is required.")
    String vdsId;
}
