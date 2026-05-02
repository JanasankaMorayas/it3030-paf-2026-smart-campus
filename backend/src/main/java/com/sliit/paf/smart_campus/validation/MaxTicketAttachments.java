package com.sliit.paf.smart_campus.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxTicketAttachmentsValidator.class)
public @interface MaxTicketAttachments {

    String message() default "A maximum of 3 image URLs is allowed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
