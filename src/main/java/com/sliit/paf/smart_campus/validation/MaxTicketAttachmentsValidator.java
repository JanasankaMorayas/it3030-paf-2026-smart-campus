package com.sliit.paf.smart_campus.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class MaxTicketAttachmentsValidator implements ConstraintValidator<MaxTicketAttachments, List<String>> {

    private static final int MAX_ATTACHMENTS = 3;

    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        return value == null || value.size() <= MAX_ATTACHMENTS;
    }
}
