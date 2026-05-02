package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.BulkActionResponse;
import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.service.AuthenticatedUserService;
import com.sliit.paf.smart_campus.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AuthenticatedUserService authenticatedUserService;
    private final NotificationService notificationService;

    public NotificationController(
            AuthenticatedUserService authenticatedUserService,
            NotificationService notificationService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String recipient,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(currentUser, unreadOnly, type, recipient, pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> getUnreadNotifications(
            Authentication authentication,
            @RequestParam(required = false) String recipient,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(currentUser, recipient, pageable));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<BulkActionResponse> markAllAsRead(
            Authentication authentication,
            @RequestParam(required = false) String recipient
    ) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        int updatedCount = notificationService.markAllAsRead(currentUser, recipient);
        return ResponseEntity.ok(BulkActionResponse.builder()
                .message("Notifications marked as read.")
                .updatedCount(updatedCount)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        notificationService.deleteNotification(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
