package com.rccl.middleware.guest.accounts.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NumericFormatListValidator.class)
@Documented
public @interface NumericFormatList {
    
    Class<?>[] groups() default {};
    
    String message() default "All values in the list must be in numeric format.";
    
    Class<? extends Payload>[] payload() default {};
}
