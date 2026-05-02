package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.dto.TicketResponse;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.exception.InvalidTicketStateException;
import com.sliit.paf.smart_campus.exception.TicketNotFoundException;
import com.sliit.paf.smart_campus.exception.TooManyAttachmentsException;
import com.sliit.paf.smart_campus.exception.UserNotFoundException;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.TicketSpecifications;
import com.sliit.paf.smart_campus.repository.UserRepository;
import com.sliit.paf.smart_campus.util.PageableUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "priority", "status", "location", "createdAt", "updatedAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(
            AuditLogService auditLogService,
            TicketRepository ticketRepository,
            NotificationService notificationService,
            AuthenticatedUserService authenticatedUserService,
            UserRepository userRepository
    ) {
        this.auditLogService = auditLogService;
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
        this.authenticatedUserService = authenticatedUserService;
        this.userRepository = userRepository;
    }

    public PageResponse<TicketResponse> getAllTickets(
            String status,
            String priority,
            String category,
            String reportedBy,
            String assignedTechnician,
            Authentication authentication,
            Pageable pageable
    ) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Role role = currentUser.getRole();
        Pageable sanitizedPageable = PageableUtils.sanitize(pageable, DEFAULT_SORT, ALLOWED_SORT_FIELDS);

        validateRequestedTicketFilters(currentUser, role, reportedBy, assignedTechnician);

        Specification<Ticket> visibilitySpec = switch (role) {
            case ADMIN -> (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
            case USER -> TicketSpecifications.hasReporterIdentifier(currentUser.getEmail());
            case TECHNICIAN -> TicketSpecifications.hasAssignedTechnician(currentUser.getEmail())
                    .or(TicketSpecifications.hasReporterIdentifier(currentUser.getEmail()));
        };

        Page<TicketResponse> ticketPage = ticketRepository.findAll(
                        Specification.where(visibilitySpec)
                                .and(TicketSpecifications.hasStatus(parseStatus(status)))
                                .and(TicketSpecifications.hasPriority(parsePriority(priority)))
                                .and(TicketSpecifications.hasCategory(parseCategory(category)))
                                .and(TicketSpecifications.hasReporterIdentifier(resolveReportedByFilter(role, currentUser, reportedBy)))
                                .and(TicketSpecifications.hasAssignedTechnician(resolveAssignedTechnicianFilter(role, currentUser, assignedTechnician))),
                        sanitizedPageable
                ).map(TicketResponse::from);

        return PageResponse.from(ticketPage);
    }

    public TicketResponse getTicketById(Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Ticket ticket = findTicketById(id);

        validateTicketReadAccess(ticket, currentUser);
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Role role = currentUser.getRole();
        List<String> imageUrls = normalizeImageUrls(request.getImageUrls());
        TicketReporter reporter = resolveReporterForCreate(currentUser, role, request.getReportedBy());

        Ticket ticket = Ticket.builder()
                .title(normalizeRequiredText(request.getTitle()))
                .description(normalizeRequiredText(request.getDescription()))
                .category(TicketCategory.from(request.getCategory()))
                .priority(TicketPriority.from(request.getPriority()))
                .status(TicketStatus.OPEN)
                .location(normalizeRequiredText(request.getLocation()))
                .reportedByUser(reporter.reportedByUser())
                .reportedBy(reporter.reportedBy())
                .build();

        applyImageUrls(ticket, imageUrls);

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.recordEvent(
                "TICKET",
                savedTicket.getId(),
                "TICKET_CREATED",
                currentUser,
                currentUser.getEmail(),
                "Ticket created with priority " + savedTicket.getPriority()
                        + " and category " + savedTicket.getCategory() + "."
        );
        notificationService.notifyTicketCreated(savedTicket);
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Ticket ticket = findTicketById(id);

        validateTicketReporterWriteAccess(ticket, currentUser);
        validateTicketCanBeUpdated(ticket);

        List<String> imageUrls = normalizeImageUrls(request.getImageUrls());
        TicketReporter reporter = resolveReporterForUpdate(ticket, currentUser, request.getReportedBy());

        ticket.setTitle(normalizeRequiredText(request.getTitle()));
        ticket.setDescription(normalizeRequiredText(request.getDescription()));
        ticket.setCategory(TicketCategory.from(request.getCategory()));
        ticket.setPriority(TicketPriority.from(request.getPriority()));
        ticket.setLocation(normalizeRequiredText(request.getLocation()));
        ticket.setReportedByUser(reporter.reportedByUser());
        ticket.setReportedBy(reporter.reportedBy());
        applyImageUrls(ticket, imageUrls);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long id, UpdateTicketStatusRequest request, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Role role = currentUser.getRole();
        Ticket ticket = findTicketById(id);
        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus targetStatus = TicketStatus.from(request.getStatus());
        String resolutionNotes = normalizeNullableText(request.getResolutionNotes());

        validateTicketStatusAccess(ticket, currentUser, role, targetStatus);
        validateStatusTransitionForActor(ticket, currentUser, role, targetStatus);
        validateResolutionRequirements(ticket, targetStatus, resolutionNotes);

        ticket.setStatus(targetStatus);
        if (resolutionNotes != null) {
            ticket.setResolutionNotes(resolutionNotes);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.recordEvent(
                "TICKET",
                savedTicket.getId(),
                "TICKET_STATUS_CHANGED",
                currentUser,
                currentUser.getEmail(),
                "Ticket status changed from " + currentStatus + " to " + targetStatus + "."
        );
        notificationService.notifyTicketStatusChanged(savedTicket);
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse assignTechnician(Long id, AssignTechnicianRequest request, Authentication authentication) {
        requireAdmin(authentication);
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Ticket ticket = findTicketById(id);

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be assigned to a technician.");
        }

        User technicianUser = resolveAssignableTechnician(normalizeRequiredText(request.getAssignedTechnician()));
        ticket.setAssignedTechnicianUser(technicianUser);
        ticket.setAssignedTechnician(technicianUser.getEmail());

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.recordEvent(
                "TICKET",
                savedTicket.getId(),
                "TICKET_ASSIGNED",
                currentUser,
                currentUser.getEmail(),
                "Ticket assigned to technician " + technicianUser.getEmail() + "."
        );
        notificationService.notifyTicketAssigned(savedTicket);
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public void deleteTicket(Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        Ticket ticket = findTicketById(id);

        validateTicketReporterWriteAccess(ticket, currentUser);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return;
        }

        TicketStatus previousStatus = ticket.getStatus();

        if (previousStatus != TicketStatus.OPEN && previousStatus != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be cancelled.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.recordEvent(
                "TICKET",
                savedTicket.getId(),
                "TICKET_STATUS_CHANGED",
                currentUser,
                currentUser.getEmail(),
                "Ticket status changed from " + previousStatus + " to CANCELLED."
        );
        notificationService.notifyTicketStatusChanged(savedTicket != null ? savedTicket : ticket);
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    private void requireAdmin(Authentication authentication) {
        if (!authenticatedUserService.isAdmin(authentication)) {
            throw new AccessDeniedException("Admin access is required.");
        }
    }

    private User resolveAssignableTechnician(String assignedTechnician) {
        User technicianUser = userRepository.findByEmailIgnoreCase(assignedTechnician)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + assignedTechnician));

        if (!Boolean.TRUE.equals(technicianUser.getActive())) {
            throw new IllegalArgumentException("Assigned technician user must be active.");
        }

        if (technicianUser.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Assigned user must have TECHNICIAN role.");
        }

        return technicianUser;
    }

    private void validateTicketReadAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN || isReporter(ticket, currentUser) || isAssignedTechnician(ticket, currentUser)) {
            return;
        }

        throw new AccessDeniedException("You can only access tickets you reported or that are assigned to you.");
    }

    private void validateTicketReporterWriteAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN || isReporter(ticket, currentUser)) {
            return;
        }

        throw new AccessDeniedException("Only the reporting user or an admin can modify this ticket.");
    }

    private void validateTicketStatusAccess(Ticket ticket, User currentUser, Role role, TicketStatus targetStatus) {
        if (role == Role.ADMIN) {
            return;
        }

        if (role == Role.TECHNICIAN && isAssignedTechnician(ticket, currentUser)) {
            return;
        }

        if (isReporter(ticket, currentUser) && targetStatus == TicketStatus.CANCELLED) {
            return;
        }

        if (isReporter(ticket, currentUser)) {
            throw new AccessDeniedException("Only admins or the assigned technician can set this ticket status.");
        }

        throw new AccessDeniedException("You can only update the status of tickets assigned to you.");
    }

    private void validateRequestedTicketFilters(
            User currentUser,
            Role role,
            String reportedBy,
            String assignedTechnician
    ) {
        if (role == Role.USER && StringUtils.hasText(reportedBy)
                && !reportedBy.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("You can only view your own tickets.");
        }

        if (role == Role.TECHNICIAN && StringUtils.hasText(assignedTechnician)
                && !assignedTechnician.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("Technicians can only filter tickets assigned to themselves.");
        }
    }

    private void validateStatusTransitionForActor(Ticket ticket, User currentUser, Role role, TicketStatus targetStatus) {
        if (role == Role.ADMIN) {
            validateGeneralStatusTransition(ticket.getStatus(), targetStatus);
            return;
        }

        if (role == Role.TECHNICIAN && isAssignedTechnician(ticket, currentUser)) {
            validateTechnicianStatusTransition(ticket.getStatus(), targetStatus);
            return;
        }

        if (isReporter(ticket, currentUser) && targetStatus == TicketStatus.CANCELLED) {
            validateGeneralStatusTransition(ticket.getStatus(), targetStatus);
            return;
        }

        throw new AccessDeniedException("This status transition is not allowed for your role.");
    }

    private boolean isReporter(Ticket ticket, User user) {
        if (ticket.getReportedByUser() != null && ticket.getReportedByUser().getEmail() != null
                && ticket.getReportedByUser().getEmail().equalsIgnoreCase(user.getEmail())) {
            return true;
        }

        return StringUtils.hasText(ticket.getReportedBy())
                && ticket.getReportedBy().trim().equalsIgnoreCase(user.getEmail());
    }

    private boolean isAssignedTechnician(Ticket ticket, User user) {
        if (ticket.getAssignedTechnicianUser() != null && ticket.getAssignedTechnicianUser().getEmail() != null
                && ticket.getAssignedTechnicianUser().getEmail().equalsIgnoreCase(user.getEmail())) {
            return true;
        }

        return StringUtils.hasText(ticket.getAssignedTechnician())
                && ticket.getAssignedTechnician().trim().equalsIgnoreCase(user.getEmail());
    }

    private String resolveReportedByFilter(Role role, User currentUser, String reportedBy) {
        if (role == Role.USER) {
            return currentUser.getEmail();
        }

        return normalizeNullableText(reportedBy);
    }

    private String resolveAssignedTechnicianFilter(Role role, User currentUser, String assignedTechnician) {
        if (role == Role.TECHNICIAN) {
            return currentUser.getEmail();
        }

        return normalizeNullableText(assignedTechnician);
    }

    private TicketReporter resolveReporterForCreate(User currentUser, Role role, String reportedBy) {
        if (role != Role.ADMIN) {
            validateRequestedIdentifierMatchesCurrentUser(reportedBy, currentUser, "create tickets");
            return new TicketReporter(currentUser, currentUser.getEmail());
        }

        if (!StringUtils.hasText(reportedBy)) {
            return new TicketReporter(currentUser, currentUser.getEmail());
        }

        return mapReporterFromIdentifier(normalizeRequiredText(reportedBy));
    }

    private TicketReporter resolveReporterForUpdate(Ticket ticket, User currentUser, String reportedBy) {
        if (currentUser.getRole() != Role.ADMIN) {
            validateRequestedIdentifierMatchesCurrentUser(reportedBy, currentUser, "update tickets");
            return new TicketReporter(ticket.getReportedByUser(), ticket.getReportedBy());
        }

        if (!StringUtils.hasText(reportedBy)) {
            return new TicketReporter(ticket.getReportedByUser(), ticket.getReportedBy());
        }

        return mapReporterFromIdentifier(normalizeRequiredText(reportedBy));
    }

    private TicketReporter mapReporterFromIdentifier(String reportedBy) {
        User reportedByUser = userRepository.findByEmailIgnoreCase(reportedBy).orElse(null);
        return new TicketReporter(reportedByUser, reportedByUser != null ? reportedByUser.getEmail() : reportedBy);
    }

    private void validateRequestedIdentifierMatchesCurrentUser(String reportedBy, User currentUser, String action) {
        if (StringUtils.hasText(reportedBy) && !reportedBy.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("You can only " + action + " for your own account.");
        }
    }

    private void validateTicketCanBeUpdated(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be updated.");
        }
    }

    private void validateGeneralStatusTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case OPEN -> targetStatus == TicketStatus.IN_PROGRESS
                    || targetStatus == TicketStatus.RESOLVED
                    || targetStatus == TicketStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == TicketStatus.RESOLVED
                    || targetStatus == TicketStatus.CANCELLED;
            case RESOLVED -> targetStatus == TicketStatus.CLOSED
                    || targetStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidTicketStateException(
                    "Invalid ticket status transition from " + currentStatus + " to " + targetStatus + "."
            );
        }
    }

    private void validateTechnicianStatusTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case OPEN -> targetStatus == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> targetStatus == TicketStatus.RESOLVED;
            case RESOLVED -> targetStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidTicketStateException(
                    "Technicians can only move tickets through OPEN -> IN_PROGRESS -> RESOLVED, or reopen RESOLVED tickets."
            );
        }
    }

    private void validateResolutionRequirements(Ticket ticket, TicketStatus targetStatus, String resolutionNotes) {
        if (targetStatus == TicketStatus.RESOLVED && !StringUtils.hasText(resolutionNotes)) {
            throw new InvalidTicketStateException("Resolution notes are required when resolving a ticket.");
        }

        if (targetStatus == TicketStatus.CLOSED
                && !StringUtils.hasText(resolutionNotes)
                && !StringUtils.hasText(ticket.getResolutionNotes())) {
            throw new InvalidTicketStateException("Resolution notes are required before closing a ticket.");
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        List<String> normalizedImageUrls = imageUrls.stream()
                .map(String::trim)
                .toList();

        if (normalizedImageUrls.size() > 3) {
            throw new TooManyAttachmentsException("A maximum of 3 image URLs is allowed.");
        }

        return normalizedImageUrls;
    }

    private void applyImageUrls(Ticket ticket, List<String> imageUrls) {
        ticket.setImageUrl1(imageUrls.size() > 0 ? imageUrls.get(0) : null);
        ticket.setImageUrl2(imageUrls.size() > 1 ? imageUrls.get(1) : null);
        ticket.setImageUrl3(imageUrls.size() > 2 ? imageUrls.get(2) : null);
    }

    private TicketStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return TicketStatus.from(status);
    }

    private TicketPriority parsePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return null;
        }
        return TicketPriority.from(priority);
    }

    private TicketCategory parseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        return TicketCategory.from(category);
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record TicketReporter(User reportedByUser, String reportedBy) {
    }
}
