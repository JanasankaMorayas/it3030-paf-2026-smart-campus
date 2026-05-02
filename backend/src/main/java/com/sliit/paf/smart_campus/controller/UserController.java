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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserService userService;
    private final com.sliit.paf.smart_campus.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired(required = false)
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    public UserController(AuthenticatedUserService authenticatedUserService, UserService userService, com.sliit.paf.smart_campus.repository.UserRepository userRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.authenticatedUserService = authenticatedUserService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(AuthUserResponse.from(authenticatedUserService.getCurrentUser(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {
        String newName = payload.get("displayName");
        String newPassword = payload.get("password");
        
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Display name cannot be empty"));
        }

        try {
            com.sliit.paf.smart_campus.model.User user = authenticatedUserService.getCurrentUser(authentication);
            user.setDisplayName(newName);
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
            
            userRepository.save(user);
            return ResponseEntity.ok(AuthUserResponse.from(user));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to update profile: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String displayName = payload.get("displayName");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty() || displayName == null || displayName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required"));
        }

        try {
            java.util.Optional<com.sliit.paf.smart_campus.model.User> existing = userRepository.findByEmailIgnoreCase(email.trim());
            
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered."));
            }

            com.sliit.paf.smart_campus.model.User newUser = com.sliit.paf.smart_campus.model.User.builder()
                    .email(email.trim().toLowerCase(java.util.Locale.ROOT))
                    .displayName(displayName.trim())
                    .password(passwordEncoder.encode(password.trim()))
                    .role(com.sliit.paf.smart_campus.model.Role.USER)
                    .provider("LOCAL")
                    .providerId(email.trim().toLowerCase(java.util.Locale.ROOT))
                    .active(true)
                    .build();

            userRepository.save(newUser);

            // Audit Log එකක් සටහන් කිරීම
            try {
                jdbcTemplate.update(
                    "INSERT INTO audit_logs (entity_type, entity_id, action, performed_by_email, details, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                    "USER", newUser.getId(), "USER_REGISTERED", newUser.getEmail(), "User self-registered via sign-up page"
                );
            } catch (Exception e) {
                System.out.println("Failed to write audit log: " + e.getMessage());
            }

            // Welcome Email එක යැවීම
            sendWelcomeEmail(email.trim(), displayName.trim());

            return ResponseEntity.ok(Map.of("message", "Registration successful"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    private void sendWelcomeEmail(String email, String name) {
        String subject = "Welcome to Smart Campus!";
        String message = "Hello " + name + ",\n\nWelcome to the Smart Campus Operations Hub! Your account has been successfully created.\n\nBest Regards,\nSmart Campus Team";
        
        System.out.println("=================================================");
        System.out.println("MOCK EMAIL SEND (To: " + email + ")");
        System.out.println("Subject: " + subject);
        System.out.println("=================================================");
        
        if (mailSender != null) {
            try {
                org.springframework.mail.SimpleMailMessage mailMessage = new org.springframework.mail.SimpleMailMessage();
                mailMessage.setTo(email);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                mailSender.send(mailMessage);
            } catch (Exception e) {
                System.out.println("Failed to send real email. Error: " + e.getMessage());
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.updateUserRole(
                id,
                request,
                authenticatedUserService.getCurrentUser(authentication)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            com.sliit.paf.smart_campus.model.User currentUser = authenticatedUserService.getCurrentUser(authentication);
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "You cannot delete your own account."));
            }
            
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
            } else {
                return ResponseEntity.status(404).body(Map.of("message", "User not found."));
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete user. They are linked to existing bookings or tickets."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to delete user: " + e.getMessage()));
        }
    }
}
