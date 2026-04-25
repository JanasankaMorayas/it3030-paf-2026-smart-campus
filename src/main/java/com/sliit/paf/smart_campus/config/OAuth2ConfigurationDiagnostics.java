package com.sliit.paf.smart_campus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OAuth2ConfigurationDiagnostics implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ConfigurationDiagnostics.class);

    private final AppSecurityProperties securityProperties;

    public OAuth2ConfigurationDiagnostics(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppSecurityProperties.GoogleProperties google = securityProperties.getOauth2().getGoogle();

        if (!StringUtils.hasText(google.getClientId()) || !StringUtils.hasText(google.getClientSecret())) {
            logger.warn("Google OAuth2 login is disabled because GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET is not configured.");
            logger.warn("To enable Google login, set app.security.oauth2.google.client-id and app.security.oauth2.google.client-secret through environment variables or external properties.");
            return;
        }

        logger.info("Google OAuth2 login is enabled.");
        logger.info("Expected local redirect URI: http://localhost:8080/login/oauth2/code/google");
        logger.info("Configured redirect URI template: {}", google.getRedirectUri());
        logger.info("If Google shows 'invalid_client', check that the client ID and client secret belong to the same Google Web application OAuth client.");
        logger.info("If Google shows 'redirect_uri_mismatch', add http://localhost:8080/login/oauth2/code/google as an authorized redirect URI in Google Cloud Console.");
    }
}
