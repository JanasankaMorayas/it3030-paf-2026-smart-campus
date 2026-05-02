package com.sliit.paf.smart_campus.validation;

import java.time.LocalDateTime;

public interface AvailabilityRangeRequest {

    LocalDateTime getAvailabilityStart();

    LocalDateTime getAvailabilityEnd();
}
