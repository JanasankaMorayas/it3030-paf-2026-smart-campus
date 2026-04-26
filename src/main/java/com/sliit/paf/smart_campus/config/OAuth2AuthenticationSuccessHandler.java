package com.sliit.paf.smart_campus.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String REDIRECT_URI_SESSION_ATTRIBUTE = "smartCampus.oauth2SuccessRedirectUri";

    private final AppSecurityProperties securityProperties;

    public OAuth2AuthenticationSuccessHandler(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        setDefaultTargetUrl(securityProperties.getOauth2().getSuccessRedirectUri());
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object redirectTarget = session.getAttribute(REDIRECT_URI_SESSION_ATTRIBUTE);
            session.removeAttribute(REDIRECT_URI_SESSION_ATTRIBUTE);

            if (redirectTarget instanceof String redirectUri
                    && securityProperties.isAllowedFrontendRedirectUri(redirectUri)) {
                return redirectUri;
            }
        }

        return securityProperties.getOauth2().getSuccessRedirectUri();
    }
}
