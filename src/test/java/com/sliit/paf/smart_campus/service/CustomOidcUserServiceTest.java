package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

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
class CustomOidcUserServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Test
    void loadUser_shouldPersistOidcUserAndReturnRoleAuthority() {
        OidcUserRequest userRequest = buildUserRequest();
        OidcUser oidcUser = new DefaultOidcUser(
                List.of(),
                buildIdToken(Map.of(
                        "sub", "google-sub-123",
                        "email", "google-user@example.com",
                        "name", "Google User"
                )),
                new OidcUserInfo(Map.of(
                        "sub", "google-sub-123",
                        "email", "google-user@example.com",
                        "name", "Google User"
                )),
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

        when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
        when(userService.upsertOAuth2User(
                "google-user@example.com",
                "Google User",
                "google",
                "google-sub-123"
        )).thenReturn(persistedUser);

        CustomOidcUserService customOidcUserService = new CustomOidcUserService(userService, delegate);

        OidcUser loadedUser = customOidcUserService.loadUser(userRequest);

        assertThat(loadedUser.<String>getClaim("email")).isEqualTo("google-user@example.com");
        assertThat(loadedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        verify(userService).upsertOAuth2User("google-user@example.com", "Google User", "google", "google-sub-123");
    }

    @Test
    void loadUser_shouldThrowWhenEmailClaimIsMissing() {
        OidcUserRequest userRequest = buildUserRequest();
        OidcUser oidcUser = new DefaultOidcUser(
                List.of(),
                buildIdToken(Map.of(
                        "sub", "google-sub-123",
                        "name", "Google User"
                )),
                "sub"
        );

        when(delegate.loadUser(any(OidcUserRequest.class))).thenReturn(oidcUser);

        CustomOidcUserService customOidcUserService = new CustomOidcUserService(userService, delegate);

        assertThatThrownBy(() -> customOidcUserService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Missing required OAuth2 attribute: email");

        verify(userService, never()).upsertOAuth2User(any(), any(), any(), any());
    }

    private OidcUserRequest buildUserRequest() {
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
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-access-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        return new OidcUserRequest(clientRegistration, accessToken, buildIdToken(Map.of("sub", "google-sub-123")));
    }

    private OidcIdToken buildIdToken(Map<String, Object> claims) {
        return new OidcIdToken("mock-id-token", Instant.now(), Instant.now().plusSeconds(300), claims);
    }
}
