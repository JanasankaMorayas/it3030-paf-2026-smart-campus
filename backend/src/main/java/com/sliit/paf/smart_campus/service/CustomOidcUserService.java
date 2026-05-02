package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final UserService userService;

    @Autowired
    public CustomOidcUserService(UserService userService) {
        this(userService, new OidcUserService());
    }

    CustomOidcUserService(
            UserService userService,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate
    ) {
        this.userService = userService;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = readRequiredAttribute(attributes, "email");
        String displayName = readOptionalAttribute(attributes, "name", email);
        String providerId = readOptionalAttribute(attributes, "sub", oidcUser.getName());
        String provider = userRequest.getClientRegistration().getRegistrationId();

        User user = userService.upsertOAuth2User(email, displayName, provider, providerId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        if (oidcUser.getUserInfo() != null) {
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), "email");
    }

    private String readRequiredAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Missing required OAuth2 attribute: " + key
            );
        }
        return value.toString().trim();
    }

    private String readOptionalAttribute(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);
        return value == null || value.toString().isBlank() ? defaultValue : value.toString().trim();
    }
}
