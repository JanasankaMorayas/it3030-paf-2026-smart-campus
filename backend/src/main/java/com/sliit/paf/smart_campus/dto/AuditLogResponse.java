package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private Long performedByUserId;
    private String performedByEmail;
    private String performedByIdentifier;
    private String details;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .performedByUserId(auditLog.getPerformedByUser() != null ? auditLog.getPerformedByUser().getId() : null)
                .performedByEmail(auditLog.getPerformedByUser() != null ? auditLog.getPerformedByUser().getEmail() : auditLog.getPerformedByIdentifier())
                .performedByIdentifier(auditLog.getPerformedByIdentifier())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
