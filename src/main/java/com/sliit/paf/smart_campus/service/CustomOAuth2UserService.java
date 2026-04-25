package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserService userService;

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = readRequiredAttribute(attributes, "email");
        String displayName = readOptionalAttribute(attributes, "name", email);
        String providerId = readOptionalAttribute(attributes, "sub", oauth2User.getName());
        String provider = userRequest.getClientRegistration().getRegistrationId();

        User user = userService.upsertOAuth2User(email, displayName, provider, providerId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new DefaultOAuth2User(authorities, attributes, "email");
    }

    private String readRequiredAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new OAuth2AuthenticationException("Missing required OAuth2 attribute: " + key);
        }
        return value.toString().trim();
    }

    private String readOptionalAttribute(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);
        return value == null || value.toString().isBlank() ? defaultValue : value.toString().trim();
    }
}
