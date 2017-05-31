package com.rccl.middleware.guest.impl.accounts;

import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.exceptions.InvalidGuestException;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GuestValidator {
    
    private Validator validator;
    
    @Inject
    public GuestValidator(Validator validator) {
        this.validator = validator;
    }
    
    void validate(Guest guest) {
        validateGuestWithValidationGroup(guest, Guest.CreateChecks.class);
    }
    
    void validateGuestUpdateModel(Guest guest) {
        validateGuestWithValidationGroup(guest, Guest.UpdateChecks.class);
    }
    
    /**
     * Validate the given guest against the given validation group class, which is expected to
     * be within the {@link Guest} class itself.
     * <p>
     * If the guest is invalid, an unhandled {@link InvalidGuestException} is thrown. Otherwise,
     * nothing happens.
     *
     * @param guest                {@link Guest}
     * @param validationGroupClass {@code Class<T>}
     */
    private void validateGuestWithValidationGroup(Guest guest, Class<?> validationGroupClass) {
        Set<ConstraintViolation<Guest>> violations = validator.validate(guest, validationGroupClass);
        
        if (violations.isEmpty()) {
            return;
        }
        
        Map<String, String> violationsReport = violations.stream().collect(
                Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (duplicate1, duplicate2) -> duplicate1
                )
        );
        
        throw new InvalidGuestException(violationsReport);
    }
}
