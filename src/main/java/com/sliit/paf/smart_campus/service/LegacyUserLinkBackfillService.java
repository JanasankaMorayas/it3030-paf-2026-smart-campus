package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BackfillUserLinksResponse;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LegacyUserLinkBackfillService {

    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public LegacyUserLinkBackfillService(
            AuditLogService auditLogService,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository
    ) {
        this.auditLogService = auditLogService;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public BackfillUserLinksResponse backfillUserLinks(User performedByUser) {
        int scanned = 0;
        int linked = 0;
        int skipped = 0;
        int bookingsLinked = 0;
        int ticketReportersLinked = 0;
        int ticketTechniciansLinked = 0;

        List<Booking> bookingsToSave = new ArrayList<>();
        for (Booking booking : bookingRepository.findAllByOwnerUserIsNullAndRequesterIdIsNotNull()) {
            scanned++;
            User matchedUser = resolveUserByEmail(booking.getRequesterId());
            if (matchedUser == null) {
                skipped++;
                continue;
            }

            booking.setOwnerUser(matchedUser);
            bookingsToSave.add(booking);
            linked++;
            bookingsLinked++;
        }
        if (!bookingsToSave.isEmpty()) {
            bookingRepository.saveAll(bookingsToSave);
        }

        List<Ticket> reportedTicketsToSave = new ArrayList<>();
        for (Ticket ticket : ticketRepository.findAllByReportedByUserIsNullAndReportedByIsNotNull()) {
            scanned++;
            User matchedUser = resolveUserByEmail(ticket.getReportedBy());
            if (matchedUser == null) {
                skipped++;
                continue;
            }

            ticket.setReportedByUser(matchedUser);
            reportedTicketsToSave.add(ticket);
            linked++;
            ticketReportersLinked++;
        }
        if (!reportedTicketsToSave.isEmpty()) {
            ticketRepository.saveAll(reportedTicketsToSave);
        }

        List<Ticket> assignedTicketsToSave = new ArrayList<>();
        for (Ticket ticket : ticketRepository.findAllByAssignedTechnicianUserIsNullAndAssignedTechnicianIsNotNull()) {
            scanned++;
            User matchedUser = resolveUserByEmail(ticket.getAssignedTechnician());
            if (matchedUser == null || matchedUser.getRole() != Role.TECHNICIAN || !Boolean.TRUE.equals(matchedUser.getActive())) {
                skipped++;
                continue;
            }

            ticket.setAssignedTechnicianUser(matchedUser);
            assignedTicketsToSave.add(ticket);
            linked++;
            ticketTechniciansLinked++;
        }
        if (!assignedTicketsToSave.isEmpty()) {
            ticketRepository.saveAll(assignedTicketsToSave);
        }

        BackfillUserLinksResponse response = BackfillUserLinksResponse.builder()
                .recordsScanned(scanned)
                .recordsLinked(linked)
                .recordsSkipped(skipped)
                .bookingsLinked(bookingsLinked)
                .ticketReportersLinked(ticketReportersLinked)
                .ticketTechniciansLinked(ticketTechniciansLinked)
                .build();

        auditLogService.recordEvent(
                "SYSTEM",
                null,
                "USER_LINK_BACKFILL",
                performedByUser,
                performedByUser != null ? performedByUser.getEmail() : null,
                "Legacy user-link backfill scanned " + scanned + " records, linked " + linked + ", skipped " + skipped + "."
        );

        return response;
    }

    private User resolveUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        return userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
    }
}
