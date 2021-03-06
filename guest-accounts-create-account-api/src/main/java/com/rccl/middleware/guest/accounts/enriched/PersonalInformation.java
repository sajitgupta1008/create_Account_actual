package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.beans.Gender;
import com.rccl.middleware.common.validation.validator.Birthdate;
import com.rccl.middleware.common.validation.validator.ValidGender;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonalInformation implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @Pattern(regexp = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            message = "A valid avatar URL path is required, if being provided.")
    String avatar;
    
    @Size(min = 1, max = 50, message = "The nickname must be at least one (1) character"
            + " and maximum of fifty (50) characters.")
    @Pattern(regexp = "^[A-Za-z0-9]+[A-Za-z0-9\\s]{0,19}$",
            message = "If provided, the nickname must be at least one alphanumeric character.")
    String nickname;
    
    @Size(min = 1, max = 50, message = "The first name must be at least one (1) character"
            + " and maximum of fifty (50) characters.")
    String firstName;
    
    @Size(min = 1, max = 50, message = "The last name must be at least one (1) character"
            + " and maximum of fifty (50) characters.")
    String lastName;
    
    @Size(max = 50, message = "The middle name must have a maximum of fifty (50) characters.")
    String middleName;
    
    String suffix;
    
    String title;
    
    @Birthdate
    String birthdate;
    
    @ValidGender(allowBlank = true)
    Gender gender;
}
