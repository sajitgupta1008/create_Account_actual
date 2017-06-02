package com.rccl.middleware.guest.accounts.validation;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class NumericFormatListValidator implements ConstraintValidator<NumericFormatList, List<String>> {
    
    @Override
    public void initialize(NumericFormatList constraintAnnotation) {
        // No-op.
    }
    
    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        String nonNumericVal = value.stream()
                .filter(val -> !StringUtils.isNumeric(val))
                .findAny()
                .orElse(null);
        
        return nonNumericVal == null ? true : false;
    }
}
