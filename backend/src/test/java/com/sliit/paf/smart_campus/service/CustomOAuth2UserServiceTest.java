package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Test
    void loadUser_shouldPersistGoogleUserAndReturnRoleAuthority() {
        OAuth2UserRequest userRequest = buildUserRequest();
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "email", "google-user@example.com",
                        "name", "Google User",
                        "sub", "google-sub-123"
                ),
                "email"
        );

        User persistedUser = User.builder()
                .id(1L)
                .email("google-user@example.com")
                .displayName("Google User")
                .provider("GOOGLE")
                .providerId("google-sub-123")
                .role(Role.USER)
                .active(true)
                .build();

        when(delegate.loadUser(userRequest)).thenReturn(oauth2User);
        when(userService.upsertOAuth2User(
                "google-user@example.com",
                "Google User",
                "google",
                "google-sub-123"
        )).thenReturn(persistedUser);

        CustomOAuth2UserService customOAuth2UserService = new CustomOAuth2UserService(userService, delegate);

        OAuth2User loadedUser = customOAuth2UserService.loadUser(userRequest);

        assertThat(loadedUser.<String>getAttribute("email")).isEqualTo("google-user@example.com");
        assertThat(loadedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        verify(userService).upsertOAuth2User("google-user@example.com", "Google User", "google", "google-sub-123");
    }

    @Test
    void loadUser_shouldThrowWhenEmailAttributeIsMissing() {
        OAuth2UserRequest userRequest = buildUserRequest();
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "name", "Google User",
                        "sub", "google-sub-123"
                ),
                "sub"
        );

        when(delegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(oauth2User);

        CustomOAuth2UserService customOAuth2UserService = new CustomOAuth2UserService(userService, delegate);

        assertThatThrownBy(() -> customOAuth2UserService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Missing required OAuth2 attribute: email");

        verify(userService, never()).upsertOAuth2User(any(), any(), any(), any());
    }

    private OAuth2UserRequest buildUserRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientName("Google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .scope("openid", "profile", "email")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-access-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}
