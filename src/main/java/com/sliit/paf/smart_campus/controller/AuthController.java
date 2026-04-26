package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.config.AppSecurityProperties;
import com.sliit.paf.smart_campus.config.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

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
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            HttpServletRequest request
    ) {
        String resolvedRedirectUri = resolveRedirectUri(redirectUri);

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            if (StringUtils.hasText(redirectUri) && !securityProperties.isAllowedFrontendRedirectUri(redirectUri)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("message", "Unsupported frontend redirect URI.");
                body.put("redirectUri", redirectUri);
                body.put("allowedFrontendOrigins", securityProperties.getDevFrontendOrigins());
                return ResponseEntity.badRequest().body(body);
            }

            request.getSession(true).setAttribute(
                    OAuth2AuthenticationSuccessHandler.REDIRECT_URI_SESSION_ATTRIBUTE,
                    resolvedRedirectUri
            );

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
        if (!StringUtils.hasText(requestedRedirectUri)) {
            return securityProperties.getOauth2().getSuccessRedirectUri();
        }

        return securityProperties.isAllowedFrontendRedirectUri(requestedRedirectUri)
                ? requestedRedirectUri
                : securityProperties.getOauth2().getSuccessRedirectUri();
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
        body.put("expectedAlternateRedirectUri", "http://127.0.0.1:8080/login/oauth2/code/google");
        body.put("successRedirectUri", resolvedRedirectUri);
        body.put("allowedFrontendOrigins", securityProperties.getDevFrontendOrigins());
        return body;
    }
}
