package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.exception.UserNotFoundException;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Locale;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final UserService userService;

    public AuthenticatedUserService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public String getCurrentUserEmail(Authentication authentication) {
        String email = extractEmail(authentication);
        return normalizeEmail(email);
    }

    public User getCurrentUser(Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> syncAuthenticatedOAuthUser(authentication, email));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
            Object email = oauth2Principal.getAttributes().get("email");
            if (email != null && !email.toString().isBlank()) {
                return email.toString().trim();
            }
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName().trim();
        }

        throw new AccessDeniedException("Unable to resolve the authenticated user.");
    }

    private User syncAuthenticatedOAuthUser(Authentication authentication, String normalizedEmail) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken
                && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
            Map<String, Object> attributes = oauth2Principal.getAttributes();
            String displayName = readOptionalAttribute(attributes, "name", normalizedEmail);
            String providerId = readOptionalAttribute(attributes, "sub", oauth2Principal.getName());
            String provider = oauth2AuthenticationToken.getAuthorizedClientRegistrationId();
            return userService.upsertOAuth2User(normalizedEmail, displayName, provider, providerId);
        }

        throw new UserNotFoundException("User not found with email: " + normalizedEmail);
    }

    private String readOptionalAttribute(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);
        return value == null || value.toString().isBlank() ? defaultValue : value.toString().trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
