package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AuthUserResponse;
import com.sliit.paf.smart_campus.dto.UpdateUserRoleRequest;
import com.sliit.paf.smart_campus.dto.UserResponse;
import com.sliit.paf.smart_campus.exception.UserNotFoundException;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String LOCAL_DEV_PROVIDER = "LOCAL_DEV";

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public AuthUserResponse getCurrentUser(String email) {
        return AuthUserResponse.from(findUserByEmail(email));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request, User performedByUser) {
        User user = findUserById(id);
        Role previousRole = user.getRole();
        Role updatedRole = Role.from(request.getRole());

        user.setRole(updatedRole);
        User savedUser = userRepository.save(user);

        auditLogService.recordEvent(
                "USER",
                savedUser.getId(),
                "ROLE_CHANGED",
                performedByUser,
                performedByUser != null ? performedByUser.getEmail() : null,
                "Role changed from " + previousRole + " to " + updatedRole + " for user " + savedUser.getEmail() + "."
        );

        return UserResponse.from(savedUser);
    }

    @Transactional
    public void ensureLocalUserExists(String email, String displayName, Role role) {
        upsertUser(email, displayName, LOCAL_DEV_PROVIDER, normalizeEmail(email), role);
    }

    @Transactional
    public User upsertOAuth2User(String email, String displayName, String provider, String providerId) {
        return upsertUser(email, displayName, provider, providerId, Role.USER);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    private User upsertUser(String email, String displayName, String provider, String providerId, Role defaultRole) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedProvider = provider.trim().toUpperCase(Locale.ROOT);
        String normalizedProviderId = providerId.trim();

        User user = userRepository.findByProviderAndProviderId(normalizedProvider, normalizedProviderId)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .role(defaultRole)
                        .build());

        if (user.getRole() == null) {
            user.setRole(defaultRole);
        }

        user.setDisplayName(displayName.trim());
        user.setProvider(normalizedProvider);
        user.setProviderId(normalizedProviderId);
        user.setActive(true);

        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
