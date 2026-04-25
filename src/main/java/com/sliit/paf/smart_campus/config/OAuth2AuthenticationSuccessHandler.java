package com.sliit.paf.smart_campus.config;

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public OAuth2AuthenticationSuccessHandler(AppSecurityProperties securityProperties) {
        setDefaultTargetUrl(securityProperties.getOauth2().getSuccessRedirectUri());
        setAlwaysUseDefaultTargetUrl(false);
    }
}
