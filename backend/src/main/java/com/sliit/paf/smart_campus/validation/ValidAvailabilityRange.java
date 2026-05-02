package com.sliit.paf.smart_campus.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AvailabilityRangeValidator.class)
public @interface ValidAvailabilityRange {

    String message() default "Invalid availability range.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
