package com.sliit.paf.smart_campus.validation;

import java.time.LocalDateTime;

public interface BookingTimeRangeRequest {

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();
}
