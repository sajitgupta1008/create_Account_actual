package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guest.accounts.validation.Brand;
import com.rccl.middleware.guest.accounts.validation.DateFormat;
import com.rccl.middleware.guest.accounts.validation.GuestAccountPassword;
import com.rccl.middleware.guest.accounts.validation.NumericFormatList;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Guest implements Jsonable {
    
    @NotNull(message = "A brand char is required.", groups = CreateChecks.class)
    @Brand(groups = DefaultChecks.class)
    Character brand;
    
    @NotBlank(message = "An email is required.", groups = DefaultChecks.class)
    @Email(regexp = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
            message = "The email is invalidly formatted.",
            groups = DefaultChecks.class)
    String email;
    
    @NotNull(message = "A first name is required.", groups = CreateChecks.class)
    @Size(min = 2, max = 100, message = "The first name must be at least two (2) characters.", groups = DefaultChecks.class)
    String firstName;
    
    @NotNull(message = "A last name is required.", groups = CreateChecks.class)
    @Size(min = 2, max = 100, message = "The last name must be at least two (2) characters.", groups = DefaultChecks.class)
    String lastName;
    
    @NotNull(message = "A date of birth is required.", groups = CreateChecks.class)
    @DateFormat(groups = DefaultChecks.class)
    String dateOfBirth;
    
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
    
    public interface CreateChecks extends DefaultChecks {
        // Validation group interface.
    }
    
    public interface UpdateChecks extends DefaultChecks {
        // Validation group interface.
    }
    
    interface DefaultChecks {
        // Validation group interface.
    }
}
