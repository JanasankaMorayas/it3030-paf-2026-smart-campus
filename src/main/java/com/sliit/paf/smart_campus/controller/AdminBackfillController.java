package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.BackfillUserLinksResponse;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.service.AuthenticatedUserService;
import com.sliit.paf.smart_campus.service.LegacyUserLinkBackfillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/backfill")
public class AdminBackfillController {

    private final AuthenticatedUserService authenticatedUserService;
    private final LegacyUserLinkBackfillService legacyUserLinkBackfillService;

    public AdminBackfillController(
            AuthenticatedUserService authenticatedUserService,
            LegacyUserLinkBackfillService legacyUserLinkBackfillService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.legacyUserLinkBackfillService = legacyUserLinkBackfillService;
    }

    @PostMapping("/user-links")
    public ResponseEntity<BackfillUserLinksResponse> backfillUserLinks(Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        return ResponseEntity.ok(legacyUserLinkBackfillService.backfillUserLinks(currentUser));
    }
}
