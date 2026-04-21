package com.sliit.paf.smart_campus.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Getter
@Builder
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private Map<String, String> validationErrors = Collections.emptyMap();
}
