package com.rccl.middleware.guest.accounts.validation;

import com.rccl.middleware.guest.accounts.Optin;
import com.rccl.middleware.guest.accounts.OptinEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class OptinValidator implements ConstraintValidator<OptinList, List<Optin>> {
    
    @Override
    public void initialize(OptinList constraintAnnotation) {
        // No-op.
    }
    
    @Override
    public boolean isValid(List<Optin> values, ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        
        try {
            values.forEach(optin -> OptinEnum.valueOf(optin.getType()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
