package com.sliit.paf.smart_campus.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class AvailabilityRangeValidator implements ConstraintValidator<ValidAvailabilityRange, AvailabilityRangeRequest> {

    @Override
    public boolean isValid(AvailabilityRangeRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDateTime availabilityStart = value.getAvailabilityStart();
        LocalDateTime availabilityEnd = value.getAvailabilityEnd();

        if ((availabilityStart == null) != (availabilityEnd == null)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Availability start and end must both be provided.")
                    .addPropertyNode("availabilityEnd")
                    .addConstraintViolation();
            return false;
        }

        if (availabilityStart != null && availabilityEnd != null && availabilityEnd.isBefore(availabilityStart)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Availability end must be after or equal to availability start.")
                    .addPropertyNode("availabilityEnd")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
