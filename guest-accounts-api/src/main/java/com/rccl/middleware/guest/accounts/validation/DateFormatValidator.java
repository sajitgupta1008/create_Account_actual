package com.rccl.middleware.guest.accounts.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateFormatValidator implements ConstraintValidator<DateFormat, String> {
    
    private String dateFormat;
    
    @Override
    public void initialize(DateFormat constraintAnnotation) {
        dateFormat = constraintAnnotation.format();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.dateFormat);
            simpleDateFormat.parse(value);
            return true;
            
        } catch (ParseException ex) {
            return false;
        }
    }
}
