package com.rccl.middleware.guest.accounts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityQuestion implements Jsonable {
    
    /**
     * The provided answer to the security question.
     */
    @NotNull(message = "An answer to the security question is required.", groups = Guest.DefaultChecks.class)
    @Size(min = 3, max = 100, message = "The answer should be at least three (3) characters and "
            + "must not exceed one hundred (100) characters.", groups = Guest.DefaultChecks.class)
    String answer;
    
    /**
     * A unique identifier for the security question.
     * <p>
     * TODO: Update this once the IDs are implemented for the security questions.
     */
    String id;
    
    /**
     * The content text for the question.
     */
    @NotNull(message = "A question in security question is required.",
            groups = Guest.DefaultChecks.class)
    @Size(min = 3, message = "The question should be at least three (3) characters.",
            groups = Guest.DefaultChecks.class)
    String question;
}
