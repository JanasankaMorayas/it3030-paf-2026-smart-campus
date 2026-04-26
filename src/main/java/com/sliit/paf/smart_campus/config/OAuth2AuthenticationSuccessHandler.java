package com.sliit.paf.smart_campus.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String REDIRECT_URI_SESSION_ATTRIBUTE = "oauth2_success_redirect_uri";
    private final String fallbackRedirectUri;

    public OAuth2AuthenticationSuccessHandler(AppSecurityProperties securityProperties) {
        this.fallbackRedirectUri = securityProperties.getOauth2().getSuccessRedirectUri();
        setDefaultTargetUrl(fallbackRedirectUri);
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.Authentication authentication
    ) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);

        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(REDIRECT_URI_SESSION_ATTRIBUTE);
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        Object redirectUri = request.getSession(false) != null
                ? request.getSession(false).getAttribute(REDIRECT_URI_SESSION_ATTRIBUTE)
                : null;

        if (redirectUri instanceof String targetUrl && !targetUrl.isBlank()) {
            return targetUrl;
        }

        return fallbackRedirectUri;
    }
}
