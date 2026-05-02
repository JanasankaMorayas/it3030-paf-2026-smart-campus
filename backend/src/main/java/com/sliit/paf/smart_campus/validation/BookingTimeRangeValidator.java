package com.sliit.paf.smart_campus.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class BookingTimeRangeValidator implements ConstraintValidator<ValidBookingTimeRange, BookingTimeRangeRequest> {

    @Override
    public boolean isValid(BookingTimeRangeRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDateTime startTime = value.getStartTime();
        LocalDateTime endTime = value.getEndTime();

        if (startTime == null || endTime == null) {
            return true;
        }

        if (!startTime.isBefore(endTime)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("End time must be after start time.")
                    .addPropertyNode("endTime")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
