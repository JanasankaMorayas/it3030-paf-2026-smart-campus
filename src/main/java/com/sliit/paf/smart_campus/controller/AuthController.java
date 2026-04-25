package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.config.AppSecurityProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<?> login() {
        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/oauth2/authorization/google"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("googleOauthConfigured", false);
        body.put("message", "Google OAuth2 login is not configured yet.");
        body.put("requiredEnvironmentVariables", new String[]{"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"});
        body.put("expectedRedirectUri", "http://localhost:8080/login/oauth2/code/google");
        body.put("successRedirectUri", securityProperties.getOauth2().getSuccessRedirectUri());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
