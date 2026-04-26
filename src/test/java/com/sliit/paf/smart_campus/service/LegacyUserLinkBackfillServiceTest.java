package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BackfillUserLinksResponse;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyUserLinkBackfillServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LegacyUserLinkBackfillService legacyUserLinkBackfillService;

    @Test
    void backfillUserLinks_shouldLinkMatchingUsersAndSkipInvalidTechnicianRecords() {
        User adminUser = buildUser(1L, "admin@example.com", Role.ADMIN);
        User bookingOwner = buildUser(2L, "owner@example.com", Role.USER);
        User reporter = buildUser(3L, "reporter@example.com", Role.USER);
        User technician = buildUser(4L, "tech@example.com", Role.TECHNICIAN);
        User nonTechnician = buildUser(5L, "not-tech@example.com", Role.USER);

        Booking booking = Booking.builder()
                .id(10L)
                .requesterId("owner@example.com")
                .build();

        Ticket reportedTicket = Ticket.builder()
                .id(20L)
                .reportedBy("reporter@example.com")
                .build();

        Ticket assignedTicket = Ticket.builder()
                .id(30L)
                .assignedTechnician("tech@example.com")
                .build();

        Ticket skippedAssignedTicket = Ticket.builder()
                .id(31L)
                .assignedTechnician("not-tech@example.com")
                .build();

        when(bookingRepository.findAllByOwnerUserIsNullAndRequesterIdIsNotNull()).thenReturn(List.of(booking));
        when(ticketRepository.findAllByReportedByUserIsNullAndReportedByIsNotNull()).thenReturn(List.of(reportedTicket));
        when(ticketRepository.findAllByAssignedTechnicianUserIsNullAndAssignedTechnicianIsNotNull())
                .thenReturn(List.of(assignedTicket, skippedAssignedTicket));
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(bookingOwner));
        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findByEmailIgnoreCase("tech@example.com")).thenReturn(Optional.of(technician));
        when(userRepository.findByEmailIgnoreCase("not-tech@example.com")).thenReturn(Optional.of(nonTechnician));

        BackfillUserLinksResponse response = legacyUserLinkBackfillService.backfillUserLinks(adminUser);

        assertThat(booking.getOwnerUser()).isEqualTo(bookingOwner);
        assertThat(reportedTicket.getReportedByUser()).isEqualTo(reporter);
        assertThat(assignedTicket.getAssignedTechnicianUser()).isEqualTo(technician);
        assertThat(skippedAssignedTicket.getAssignedTechnicianUser()).isNull();
        assertThat(response.getRecordsScanned()).isEqualTo(4);
        assertThat(response.getRecordsLinked()).isEqualTo(3);
        assertThat(response.getRecordsSkipped()).isEqualTo(1);
        assertThat(response.getBookingsLinked()).isEqualTo(1);
        assertThat(response.getTicketReportersLinked()).isEqualTo(1);
        assertThat(response.getTicketTechniciansLinked()).isEqualTo(1);
        verify(bookingRepository).saveAll(any());
        verify(ticketRepository, times(2)).saveAll(any());
        verify(auditLogService).recordEvent(any(), any(), any(), any(), any(), any());
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .displayName(email)
                .provider("LOCAL_DEV")
                .providerId(email)
                .role(role)
                .active(true)
                .build();
    }
}
