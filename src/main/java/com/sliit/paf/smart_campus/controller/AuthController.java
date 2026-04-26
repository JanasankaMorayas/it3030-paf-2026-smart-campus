package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.config.AppSecurityProperties;
import com.sliit.paf.smart_campus.config.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class AuthController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final AppSecurityProperties securityProperties;

    public AuthController(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            AppSecurityProperties securityProperties
    ) {
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            HttpSession session
    ) {
        String resolvedRedirectUri = resolveRedirectUri(redirectUri);

        if (isGoogleOauthConfigured()) {
            session.setAttribute(OAuth2AuthenticationSuccessHandler.REDIRECT_URI_SESSION_ATTRIBUTE, resolvedRedirectUri);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/oauth2/authorization/google"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildAuthStatusBody(false, resolvedRedirectUri));
    }

    @GetMapping("/api/auth/status")
    public ResponseEntity<Map<String, Object>> authStatus(
            @RequestParam(name = "redirect_uri", required = false) String redirectUri
    ) {
        String resolvedRedirectUri = resolveRedirectUri(redirectUri);
        return ResponseEntity.ok(buildAuthStatusBody(isGoogleOauthConfigured(), resolvedRedirectUri));
    }

    private String resolveRedirectUri(String requestedRedirectUri) {
        if (requestedRedirectUri == null || requestedRedirectUri.isBlank()) {
            return securityProperties.getOauth2().getSuccessRedirectUri();
        }

        try {
            URI redirectUri = URI.create(requestedRedirectUri);
            String origin = redirectUri.getScheme() + "://" + redirectUri.getAuthority();
            Set<String> allowedOrigins = new HashSet<>(securityProperties.getAllowedFrontendOrigins());
            if (allowedOrigins.contains(origin)) {
                return requestedRedirectUri;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to the default configured redirect target below.
        }

        return securityProperties.getOauth2().getSuccessRedirectUri();
    }

    private boolean isGoogleOauthConfigured() {
        return clientRegistrationRepositoryProvider.getIfAvailable() != null;
    }

    private Map<String, Object> buildAuthStatusBody(boolean googleOauthConfigured, String resolvedRedirectUri) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("googleOauthConfigured", googleOauthConfigured);
        body.put("message", googleOauthConfigured
                ? "Google OAuth2 login is configured."
                : "Google OAuth2 login is not configured yet.");
        body.put("requiredEnvironmentVariables", new String[]{"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"});
        body.put("expectedRedirectUri", "http://localhost:8080/login/oauth2/code/google");
        body.put("successRedirectUri", resolvedRedirectUri);
        body.put("allowedFrontendOrigins", securityProperties.getAllowedFrontendOrigins());
        return body;
    }
}
