package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.validation.validator.Birthdate;
import com.rccl.middleware.common.validation.validator.Brand;
import com.rccl.middleware.common.validation.validator.DateFormat;
import com.rccl.middleware.common.validation.validator.GuestAccountPassword;
import com.rccl.middleware.common.validation.validator.ValidatorConstants;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Guest implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @NotNull(message = "A header is required.", groups = DefaultChecks.class)
    @Valid
    Header header;
    
    @NotBlank(message = "An email is required.", groups = DefaultChecks.class)
    @Size(min = 5, max = 100, message = "The email can have a minimum of 5 characters and "
            + "a maximum of 100 characters.", groups = DefaultChecks.class)
    @Pattern(regexp = ValidatorConstants.EMAIL_REGEXP, message = "The email is invalidly formatted.",
            groups = DefaultChecks.class)
    String email;
    
    @Pattern(regexp = "([GEC])\\d+", message = "The VDS ID is invalidly formatted.", groups = DefaultChecks.class)
    @Size(max = 9, message = "The VDS ID can have a maximum of 9 characters.", groups = DefaultChecks.class)
    String vdsId;
    
    @NotNull(message = "A first name is required.", groups = CreateChecks.class)
    @Size(min = 1, max = 50, message = "The first name must be at least one (1) character"
            + " and maximum of fifty (50) characters.", groups = DefaultChecks.class)
    String firstName;
    
    @NotNull(message = "A last name is required.", groups = CreateChecks.class)
    @Size(min = 1, max = 50, message = "The last name must be at least one (1) character"
            + " and maximum of fifty (50) characters.", groups = DefaultChecks.class)
    String lastName;
    
    @Size(max = 50, message = "The middle name must have a maximum "
            + "of fifty (50) characters.", groups = DefaultChecks.class)
    String middleName;
    
    String suffix;
    
    @NotEmpty(message = "Date of birth is required.", groups = CreateChecks.class)
    @Birthdate(groups = DefaultChecks.class)
    String birthdate;
    
    @Size(min = 7, max = 30, message = "The phone number must be at least seven (7) characters"
            + " and maximum of thirty (30) characters.", groups = DefaultChecks.class)
    @Pattern(regexp = "[ 0-9+()-]*", message = "The phone number is invalidly formatted.", groups = DefaultChecks.class)
    String phoneNumber;
    
    @NotNull(message = "A password is required.", groups = CreateChecks.class)
    @GuestAccountPassword(groups = DefaultChecks.class)
    char[] password;
    
    @NotNull(message = "The security questions are required.", groups = CreateChecks.class)
    @NotEmpty(message = "At least one security question is required.", groups = CreateChecks.class)
    @Valid
    List<SecurityQuestion> securityQuestions;
    
    @NotNull(message = "The terms and conditions agreement fields are required.", groups = CreateChecks.class)
    @Valid
    TermsAndConditionsAgreement termsAndConditionsAgreement;
    
    @Pattern(regexp = "\\d*", message = "Consumer ID must be in numeric format.", groups = DefaultChecks.class)
    String consumerId;
    
    @Pattern(regexp = "\\d*", message = "Crown and Anchor Loyalty ID must be in numeric format.",
            groups = DefaultChecks.class)
    String crownAndAnchorId;
    
    @Pattern(regexp = "\\d*", message = "Captains Club Loyalty ID must be in numeric format.",
            groups = DefaultChecks.class)
    String captainsClubId;
    
    @Pattern(regexp = "\\d*", message = "Azamara Loyalty ID must be in numeric format.", groups = DefaultChecks.class)
    String azamaraLoyaltyId;
    
    @Pattern(regexp = "\\d*", message = "Club Royale Loyalty ID must be in numeric format.",
            groups = DefaultChecks.class)
    String clubRoyaleId;
    
    @Pattern(regexp = "\\d*", message = "Celebrity Blue Chip Loyalty ID must be in numeric format.",
            groups = DefaultChecks.class)
    String celebrityBlueChipId;
    
    @Pattern(regexp = "\\d*", message = "Webshopper ID must be in numeric format.", groups = DefaultChecks.class)
    String webshopperId;
    
    @Brand(groups = DefaultChecks.class)
    Character webshopperBrand;
    
    @Valid
    List<Optin> optins;
    
    String passportNumber;
    
    @DateFormat
    String passportExpirationDate;
    
    String creationTimestamp;
    
    public interface CreateChecks extends DefaultChecks {
        // Validation group interface.
    }
    
    public interface UpdateChecks extends DefaultChecks {
        // Validation group interface.
    }
    
    interface DefaultChecks extends Default {
        // Validation group interface.
    }
}
