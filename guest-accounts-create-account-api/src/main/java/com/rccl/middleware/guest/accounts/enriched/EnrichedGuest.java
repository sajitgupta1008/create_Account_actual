package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import com.rccl.middleware.guest.accounts.PrivacyPolicyAgreement;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.optin.EmailOptin;
import com.rccl.middleware.guest.optin.PostalOptin;
import com.rccl.middleware.guestprofiles.models.EmergencyContact;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedGuest implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
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
    TermsAndConditionsAgreement termsAndConditionsAgreement;
    
    @Valid
    PrivacyPolicyAgreement privacyPolicyAgreement;
    
    @Valid
    List<EmailOptin> emailOptins;
    
    @Valid
    List<PostalOptin> postalOptins;
    
    @Pattern(regexp = "\\d*", message = "Consumer ID must be in numeric format.")
    String consumerId;
    
    @NotNull(message = "An email is required.")
    @Size(min = 5, max = 100, message = "The email can have a minimum of 5 characters and a maximum of 100 characters.")
    @Pattern(regexp = ValidatorConstants.EMAIL_REGEXP, message = "The email is invalidly formatted.")
    String email;
    
    @NotNull(message = "A VDS ID is required.")
    @Pattern(regexp = "([GEC])\\d+", message = "The VDS ID is invalidly formatted.")
    @Size(max = 9, message = "The VDS ID can have a maximum of 9 characters.")
    String vdsId;
    
    public interface DefaultView {
        // used for Jackson @JSONView annotation
    }
    
    public interface ExtendedView extends DefaultView {
        // used for Jackson @JSONView annotation
    }
}
