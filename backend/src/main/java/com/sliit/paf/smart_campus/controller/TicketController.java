package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.dto.TicketResponse;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/tickets")
@Validated
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> getAllTickets(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String reportedBy,
            @RequestParam(required = false) String assignedTechnician,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.getAllTickets(
                status,
                priority,
                category,
                reportedBy,
                assignedTechnician,
                authentication,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicketById(id, authentication));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request, authentication));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, request, authentication));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignTechnician(
            @PathVariable Long id,
            @Valid @RequestBody AssignTechnicianRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ticketService.assignTechnician(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id, Authentication authentication) {
        ticketService.deleteTicket(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
