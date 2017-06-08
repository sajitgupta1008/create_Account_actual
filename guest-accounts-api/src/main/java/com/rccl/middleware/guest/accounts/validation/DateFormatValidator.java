package com.rccl.middleware.guest.accounts.validation;

import org.springframework.util.StringUtils;

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
        if (StringUtils.isEmpty(value)) {
            return false;
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
