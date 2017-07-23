package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.validation.validator.DateOfBirth;
import com.rccl.middleware.common.validation.validator.GuestAccountPassword;
import com.rccl.middleware.common.validation.validator.NumericFormatList;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.Email;
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
    
    @NotNull(message = "A header is required.", groups = DefaultChecks.class)
    @Valid
    Header header;
    
    @NotBlank(message = "An email is required.", groups = DefaultChecks.class)
    @Size(min = 5, max = 100, message = "The email can only have up to 100 characters.", groups = DefaultChecks.class)
    @Email(regexp = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\""
            + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])"
            + "*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]"
            + "|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:"
            + "[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])",
            message = "The email is invalidly formatted.",
            groups = DefaultChecks.class)
    String email;
    
    String vdsId;
    
    @NotNull(message = "A first name is required.", groups = CreateChecks.class)
    @Size(min = 1, max = 50, message = "The first name must be at least one (1) character"
            + " and maximum of fifty (50) characters.", groups = DefaultChecks.class)
    String firstName;
    
    @NotNull(message = "A last name is required.", groups = CreateChecks.class)
    @Size(min = 2, max = 50, message = "The last name must be at least two (2) characters"
            + " and maximum of fifty (50) characters.", groups = DefaultChecks.class)
    String lastName;
    
    @NotEmpty(message = "Date of birth is required.", groups = CreateChecks.class)
    @DateOfBirth(groups = DefaultChecks.class)
    String birthdate;
    
    @Size(min = 7, max = 30, message = "The phone number must be at least seven (7) characters"
            + " and maximum of thirty (30) characters.", groups = DefaultChecks.class)
    @Pattern(regexp = "[0-9+()-]*", message = "The phone number is invalidly formatted", groups = DefaultChecks.class)
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
    
    @Pattern(regexp = "\\d*", message = "Consumer ID must be in numeric format.", groups = UpdateChecks.class)
    String consumerId;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> crownAndAnchorIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> captainsClubIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> azamaraLoyaltyIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> clubRoyaleIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> celebrityBlueChipIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> royalWebShopperIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> celebrityWebShopperIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> azamaraWebShopperIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> royalBookingIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> celebrityBookingIds;
    
    @NumericFormatList(groups = DefaultChecks.class)
    List<String> azamaraBookingIds;
    
    @Pattern(regexp = "\\d*", message = "Booking ID must be in numeric format.", groups = UpdateChecks.class)
    String royalPrimaryBookingId;
    
    @Pattern(regexp = "\\d*", message = "Booking ID must be in numeric format.", groups = UpdateChecks.class)
    String celebrityPrimaryBookingId;
    
    @Pattern(regexp = "\\d*", message = "Booking ID must be in numeric format.", groups = UpdateChecks.class)
    String azamaraPrimaryBookingId;
    
    @NotNull(message = "At least one optin is required.", groups = CreateChecks.class)
    @Valid
    List<Optin> optins;
    
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
