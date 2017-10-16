package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.Email;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedGuest implements Jsonable {
    
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
    
    @Pattern(regexp = "\\d*", message = "Consumer ID must be in numeric format.")
    String consumerId;
    
    @NotNull(message = "An email is required.")
    @Size(min = 5, max = 256, message = "The email can only have up to 256 characters.")
    @Email(regexp = ValidatorConstants.EMAIL_REGEXP, message = "The email is invalidly formatted.")
    String email;
    
    @NotNull(message = "A VDS ID is required.")
    String vdsId;
}
