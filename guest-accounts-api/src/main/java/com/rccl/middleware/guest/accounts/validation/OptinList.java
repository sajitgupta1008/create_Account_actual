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
@Constraint(validatedBy = OptinValidator.class)
@Documented
public @interface OptinList {
    
    Class<?>[] groups() default {};
    
    String message() default "A value specified is not a valid optin.";
    
    Class<? extends Payload>[] payload() default {};
}
