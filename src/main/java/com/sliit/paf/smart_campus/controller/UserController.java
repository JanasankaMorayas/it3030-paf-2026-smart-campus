package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.AuthUserResponse;
import com.sliit.paf.smart_campus.dto.UpdateUserRoleRequest;
import com.sliit.paf.smart_campus.dto.UserResponse;
import com.sliit.paf.smart_campus.service.AuthenticatedUserService;
import com.sliit.paf.smart_campus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserService userService;

    public UserController(AuthenticatedUserService authenticatedUserService, UserService userService) {
        this.authenticatedUserService = authenticatedUserService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(AuthUserResponse.from(authenticatedUserService.getCurrentUser(authentication)));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserRole(id, request));
    }
}
