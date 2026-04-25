package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.service.AuthenticatedUserService;
import com.sliit.paf.smart_campus.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type
    ) {
        String currentUserEmail = authenticatedUserService.getCurrentUserEmail(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(currentUserEmail, unreadOnly, type));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        String currentUserEmail = authenticatedUserService.getCurrentUserEmail(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(currentUserEmail));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        String currentUserEmail = authenticatedUserService.getCurrentUserEmail(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUserEmail));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String currentUserEmail = authenticatedUserService.getCurrentUserEmail(authentication);
        notificationService.markAllAsRead(currentUserEmail);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        String currentUserEmail = authenticatedUserService.getCurrentUserEmail(authentication);
        notificationService.deleteNotification(id, currentUserEmail);
        return ResponseEntity.noContent().build();
    }
}
