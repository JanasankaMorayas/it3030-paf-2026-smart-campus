package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AuditLogResponse;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.model.AuditLog;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.AuditLogRepository;
import com.sliit.paf.smart_campus.repository.AuditLogSpecifications;
import com.sliit.paf.smart_campus.util.PageableUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "entityType", "entityId", "action", "createdAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResponse<AuditLogResponse> getAuditLogs(
            String entityType,
            String action,
            String performedBy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        validateDateRange(from, to);
        Pageable sanitizedPageable = PageableUtils.sanitize(pageable, DEFAULT_SORT, ALLOWED_SORT_FIELDS);

        Page<AuditLogResponse> auditLogPage = auditLogRepository.findAll(
                        AuditLogSpecifications.hasEntityType(entityType)
                                .and(AuditLogSpecifications.hasAction(action))
                                .and(AuditLogSpecifications.hasPerformedBy(performedBy))
                                .and(AuditLogSpecifications.createdAtOnOrAfter(from))
                                .and(AuditLogSpecifications.createdAtOnOrBefore(to)),
                        sanitizedPageable
                ).map(AuditLogResponse::from);

        return PageResponse.from(auditLogPage);
    }

    public PageResponse<AuditLogResponse> getAuditLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        Pageable sanitizedPageable = PageableUtils.sanitize(pageable, DEFAULT_SORT, ALLOWED_SORT_FIELDS);

        Page<AuditLogResponse> auditLogPage = auditLogRepository.findAll(
                        AuditLogSpecifications.hasEntityType(entityType)
                                .and(AuditLogSpecifications.hasEntityId(entityId)),
                        sanitizedPageable
                ).map(AuditLogResponse::from);

        return PageResponse.from(auditLogPage);
    }

    @Transactional
    public void recordEvent(
            String entityType,
            Long entityId,
            String action,
            User performedByUser,
            String performedByIdentifier,
            String details
    ) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(normalizeRequiredUppercase(entityType, "entityType"))
                .entityId(entityId)
                .action(normalizeRequiredUppercase(action, "action"))
                .performedByUser(performedByUser)
                .performedByIdentifier(resolvePerformedByIdentifier(performedByUser, performedByIdentifier))
                .details(normalizeNullable(details))
                .build();

        auditLogRepository.save(auditLog);
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before or equal to to date.");
        }
    }

    private String resolvePerformedByIdentifier(User performedByUser, String performedByIdentifier) {
        if (performedByUser != null && StringUtils.hasText(performedByUser.getEmail())) {
            return performedByUser.getEmail().trim().toLowerCase(Locale.ROOT);
        }

        return normalizeNullable(performedByIdentifier);
    }

    private String normalizeRequiredUppercase(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
